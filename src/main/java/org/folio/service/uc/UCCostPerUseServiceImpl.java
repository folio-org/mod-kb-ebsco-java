package org.folio.service.uc;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;

import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.parseTitleId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Promise;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.configuration.GetPackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitlePackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.client.uc.model.UCTitlePackageId;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rmapi.result.PackageCostPerUseResult;
import org.folio.rmapi.result.ResourceCostPerUseResult;
import org.folio.rmapi.result.TitleCostPerUseResult;
import org.folio.service.holdings.HoldingsService;

@Service
public class UCCostPerUseServiceImpl implements UCCostPerUseService {

  public static final String INVALID_FISCAL_YEAR_MESSAGE = "Invalid fiscalYear";
  public static final String INVALID_FISCAL_YEAR_DETAILS = "Parameter 'fiscalYear' is required";
  public static final String INVALID_PLATFORM_MESSAGE = "Invalid platform";
  public static final String INVALID_PLATFORM_DETAILS =
    "Parameter 'platform' should by one of: 'all', 'publisher', 'nonPublisher'";

  @Autowired
  private UCAuthService authService;
  @Autowired @Qualifier("nonSecuredUCSettingsService")
  private UCSettingsService settingsService;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private UCApigeeEbscoClient client;
  @Autowired
  private RMAPITemplateFactory templateFactory;

  @Autowired
  private Converter<ResourceCostPerUseResult, ResourceCostPerUse> resourceCostPerUseConverter;
  @Autowired
  private Converter<PackageCostPerUseResult, PackageCostPerUse> packageCostPerUseConverter;
  @Autowired
  private Converter<TitleCostPerUseResult, TitleCostPerUse> titleCostPerUseConverter;

  @Override
  public CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, String fiscalYear,
                                                                     Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear);
    ResourceId id = parseResourceId(resourceId);
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();
    return fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, okapiHeaders)
      .thenCompose(commonConfiguration -> {
        GetTitleUCConfiguration configuration = createGetTitleConfiguration(commonConfiguration);
        return getTitleCost(id, configuration)
          .thenApply(ucTitleCostPerUse -> ResourceCostPerUseResult.builder()
            .resourceId(id)
            .ucTitleCostPerUse(ucTitleCostPerUse)
            .configuration(configuration)
            .platformType(platformTypeHolder.getValue())
            .build()
          )
          .thenApply(resourceCostPerUseConverter::convert);
      });
  }

  @Override
  public CompletableFuture<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                               Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear);
    return templateFactory.createTemplate(okapiHeaders, Promise.promise())
      .getRmapiTemplateContext()
      .thenCompose(rmapiTemplateContext -> fetchTitleSelectedResources(titleId, rmapiTemplateContext))
      .thenCompose(customerResources -> {
        if (customerResources.isEmpty()) {
          return getEmptyTitleCostPerUse(titleId);
        } else {
          return getTitleCostPerUse(titleId, platform, fiscalYear, customerResources, okapiHeaders);
        }
      });
  }

  @Override
  public CompletableFuture<PackageCostPerUse> getPackageCostPerUse(String packageId, String platform, String fiscalYear,
                                                                   Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear);
    var id = parsePackageId(packageId);
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();
    return fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, okapiHeaders)
      .thenCompose(ucConfiguration -> {
        var configuration = createGetPackageConfiguration(ucConfiguration);
        return client.getPackageCostPerUse(valueOf(id.getPackageIdPart()), configuration)
          .thenCompose(ucPackageCostPerUse -> {
            var resultBuilder = PackageCostPerUseResult.builder()
              .packageId(packageId)
              .ucPackageCostPerUse(ucPackageCostPerUse)
              .configuration(ucConfiguration)
              .platformType(platformTypeHolder.getValue());

            var cost = ucPackageCostPerUse.getAnalysis().getCurrent().getCost();
            if (cost == null || cost.equals(NumberUtils.DOUBLE_ZERO)) {
              return templateFactory.createTemplate(okapiHeaders, Promise.promise())
                .getRmapiTemplateContext()
                .thenCompose(context -> holdingsService
                  .getHoldingsByPackageId(packageId, context.getCredentialsId(), context.getOkapiData().getTenant()))
                .thenCompose(dbHoldingInfos -> {
                  var titlePackageIds = dbHoldingInfos.stream()
                    .map(h -> new UCTitlePackageId(parseInt(h.getTitleId()), parseInt(h.getPackageId())))
                    .collect(Collectors.toSet());
                  return client
                    .getTitlePackageCostPerUse(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration))
                    .thenApply(titlePackageCost -> resultBuilder.titlePackageCostMap(titlePackageCost).build());
                });
            } else {
              return CompletableFuture.completedFuture(resultBuilder.build());
            }
          });
      })
      .thenApply(packageCostPerUseConverter::convert);
  }

  private CompletableFuture<List<CustomerResources>> fetchTitleSelectedResources(String titleId,
                                                                                 RMAPITemplateContext rmapiTemplateContext) {
    return rmapiTemplateContext.getTitlesService().retrieveTitle(parseTitleId(titleId), true)
      .thenApply(this::extractSelectedResources);
  }

  private CompletableFuture<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                                List<CustomerResources> customerResources,
                                                                Map<String, String> okapiHeaders) {
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();
    return fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, okapiHeaders)
      .thenCompose(ucConfiguration -> {
        var packageId = valueOf(customerResources.get(0).getPackageId());

        return client.getTitleCostPerUse(titleId, packageId, createGetTitleConfiguration(ucConfiguration))
          .thenCombine(fetchTitlePackagesCost(customerResources, ucConfiguration), (titleUsage, titlePackageCost) ->
            TitleCostPerUseResult.builder()
              .titleId(titleId)
              .ucTitleCostPerUse(titleUsage)
              .titlePackageCostMap(titlePackageCost)
              .customerResources(customerResources)
              .configuration(ucConfiguration)
              .platformType(platformTypeHolder.getValue())
              .build()
          )
          .thenApply(titleCostPerUseConverter::convert);
      });
  }

  private CompletableFuture<TitleCostPerUse> getEmptyTitleCostPerUse(String titleId) {
    TitleCostPerUse titleCostPerUse = new TitleCostPerUse()
      .withTitleId(titleId)
      .withType(TitleCostPerUse.Type.TITLE_COST_PER_USE);
    return CompletableFuture.completedFuture(titleCostPerUse);
  }

  private CompletableFuture<Map<String, UCCostAnalysis>> fetchTitlePackagesCost(List<CustomerResources> customerResources,
                                                                                CommonUCConfiguration ucConfiguration) {
    Set<UCTitlePackageId> titlePackageIds = customerResources.stream()
      .map(cr -> new UCTitlePackageId(cr.getTitleId(), cr.getPackageId()))
      .collect(Collectors.toSet());

    return client.getTitlePackageCostPerUse(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration));
  }

  private CompletableFuture<CommonUCConfiguration> fetchCommonConfiguration(String platform, String fiscalYear,
                                                                            MutableObject<PlatformType> platformTypeHolder,
                                                                            Map<String, String> okapiHeaders) {
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(okapiHeaders),
        (authToken, ucSettings) -> {
          if (platform == null) {
            platformTypeHolder.setValue(ucSettings.getAttributes().getPlatformType());
          } else {
            platformTypeHolder.setValue(PlatformType.fromValue(platform));
          }
          return createCommonConfiguration(ucSettings, fiscalYear, authToken);
        }
      );
  }

  private List<CustomerResources> extractSelectedResources(Title title) {
    return title.getCustomerResourcesList()
      .stream()
      .filter(CustomerResources::getIsSelected)
      .collect(Collectors.toList());
  }

  private void validateParams(String platform, String fiscalYear) {
    if (StringUtils.isBlank(fiscalYear)) {
      throw new InputValidationException(INVALID_FISCAL_YEAR_MESSAGE, INVALID_FISCAL_YEAR_DETAILS);
    }
    if (StringUtils.isNotBlank(platform)) {
      try {
        PlatformType.fromValue(platform);
      } catch (IllegalArgumentException e) {
        throw new InputValidationException(INVALID_PLATFORM_MESSAGE, INVALID_PLATFORM_DETAILS);
      }
    }
  }

  private CompletableFuture<UCTitleCostPerUse> getTitleCost(ResourceId id, GetTitleUCConfiguration configuration) {
    return client.getTitleCostPerUse(valueOf(id.getTitleIdPart()), valueOf(id.getPackageIdPart()), configuration);
  }

  private CommonUCConfiguration createCommonConfiguration(UCSettings ucSettings, String fiscalYear, String authToken) {
    return CommonUCConfiguration.builder()
      .accessToken(authToken)
      .customerKey(ucSettings.getAttributes().getCustomerKey())
      .analysisCurrency(ucSettings.getAttributes().getCurrency())
      .fiscalMonth(ucSettings.getAttributes().getStartMonth().value())
      .fiscalYear(fiscalYear)
      .build();
  }

  private GetTitleUCConfiguration createGetTitleConfiguration(CommonUCConfiguration ucConfiguration) {
    return GetTitleUCConfiguration.builder()
      .accessToken(ucConfiguration.getAccessToken())
      .customerKey(ucConfiguration.getCustomerKey())
      .analysisCurrency(ucConfiguration.getAnalysisCurrency())
      .fiscalMonth(ucConfiguration.getFiscalMonth())
      .fiscalYear(ucConfiguration.getFiscalYear())
      .aggregatedFullText(true)
      .build();
  }

  private GetPackageUCConfiguration createGetPackageConfiguration(CommonUCConfiguration ucConfiguration) {
    return GetPackageUCConfiguration.builder()
      .accessToken(ucConfiguration.getAccessToken())
      .customerKey(ucConfiguration.getCustomerKey())
      .analysisCurrency(ucConfiguration.getAnalysisCurrency())
      .fiscalMonth(ucConfiguration.getFiscalMonth())
      .fiscalYear(ucConfiguration.getFiscalYear())
      .aggregatedFullText(true)
      .build();
  }

  private GetTitlePackageUCConfiguration createGetTitlePackageConfiguration(CommonUCConfiguration ucConfiguration) {
    return GetTitlePackageUCConfiguration.builder()
      .accessToken(ucConfiguration.getAccessToken())
      .customerKey(ucConfiguration.getCustomerKey())
      .analysisCurrency(ucConfiguration.getAnalysisCurrency())
      .fiscalMonth(ucConfiguration.getFiscalMonth())
      .fiscalYear(ucConfiguration.getFiscalYear())
      .publisherPlatform(true)
      .previousYear(false)
      .build();
  }
}
