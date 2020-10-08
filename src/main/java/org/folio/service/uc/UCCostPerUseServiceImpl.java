package org.folio.service.uc;

import static java.lang.String.valueOf;

import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.resourceIdToString;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import io.vertx.core.Promise;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CostPerUseParameters;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.util.template.RMAPITemplateContextBuilder;
import org.folio.rest.util.template.RMAPITemplateFactory;

@Service
public class UCCostPerUseServiceImpl implements UCCostPerUseService {

  public static final String INVALID_FISCAL_YEAR_MESSAGE = "Invalid fiscalYear";
  public static final String INVALID_FISCAL_YEAR_DETAILS = "Parameter 'fiscalYear' is required";
  public static final String INVALID_PLATFORM_MESSAGE = "Invalid platform";
  public static final String INVALID_PLATFORM_DETAILS =
    "Parameter 'platform' should by one of: 'all', 'publisher', 'nonPublisher'";
  private final UCAuthService authService;
  private final UCSettingsService settingsService;
  private final UCApigeeEbscoClient client;
  private final ResourceCostPerUseConverter converter;

  @Autowired
  private RMAPITemplateFactory templateFactory;

  public UCCostPerUseServiceImpl(@Qualifier("nonSecuredUCSettingsService") UCSettingsService settingsService,
                                 UCAuthService authService, UCApigeeEbscoClient client,
                                 ResourceCostPerUseConverter converter) {
    this.authService = authService;
    this.settingsService = settingsService;
    this.client = client;
    this.converter = converter;
  }

  @Override
  public CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, String fiscalYear,
                                                                     Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear);
    ResourceId id = parseResourceId(resourceId);
    MutableObject<PlatformType> platformType = new MutableObject<>();
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(okapiHeaders),
        (authToken, ucSettings) -> {
          if (platform == null) {
            platformType.setValue(ucSettings.getAttributes().getPlatformType());
          } else {
            platformType.setValue(PlatformType.fromValue(platform));
          }
          return createConfiguration(fiscalYear, ucSettings, authToken);
        })
      .thenCompose(configuration -> getTitleCost(id, configuration)
        .thenApply(ucTitleCostPerUse -> convert(ucTitleCostPerUse, platformType.getValue(), id, configuration))
      );
  }

  @Override
  public CompletableFuture<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                             Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear);
    MutableObject<PlatformType> platformType = new MutableObject<>();
    return templateFactory.createTemplate(okapiHeaders, Promise.promise())
      .getRmapiTemplateContext()
      .thenCompose(rmapiTemplateContext ->

        rmapiTemplateContext.getTitlesService().retrieveTitle(Long.parseLong(titleId), true)
          .thenApply(title -> title.getCustomerResourcesList()
            .stream().filter(CustomerResources::getIsSelected).collect(Collectors.toList()))
      ).thenCompose(customerResources -> {
        if (customerResources.isEmpty()) {
          return CompletableFuture.completedFuture(new TitleCostPerUse());
        } else {
          return authService.authenticate(okapiHeaders)
            .thenCombine(settingsService.fetchByUser(okapiHeaders),
              (authToken, ucSettings) -> {
                if (platform == null) {
                  platformType.setValue(ucSettings.getAttributes().getPlatformType());
                } else {
                  platformType.setValue(PlatformType.fromValue(platform));
                }
                return createConfiguration(fiscalYear, ucSettings, authToken);
              })
            .thenCompose(configuration -> client
              .getTitleCostPerUse(titleId, valueOf(customerResources.get(0).getPackageId()), configuration)
              .thenApply(ucTitleCostPerUse -> convertTitle(ucTitleCostPerUse, platformType.getValue(), customerResources,
                configuration))
            );
        }
      });
  }

  private TitleCostPerUse convertTitle(UCTitleCostPerUse ucTitleCostPerUse, PlatformType value,
                                       List<CustomerResources> customerResources, GetTitleUCConfiguration configuration) {
    return null;
  }

  @Lookup
  public RMAPITemplateContextBuilder lookupContextBuilder() {
    return null;
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

  private ResourceCostPerUse convert(UCTitleCostPerUse ucTitleCostPerUse, PlatformType platformType, ResourceId resourceId,
                                     GetTitleUCConfiguration configuration) {
    ResourceCostPerUse resourceCostPerUse = converter.convert(ucTitleCostPerUse, platformType);
    resourceCostPerUse.getAttributes().setParameters(new CostPerUseParameters()
      .withCurrency(configuration.getAnalysisCurrency())
      .withStartMonth(Month.fromValue(configuration.getFiscalMonth()))
    );
    return resourceCostPerUse.withResourceId(resourceIdToString(resourceId));
  }

  private CompletableFuture<UCTitleCostPerUse> getTitleCost(ResourceId id, GetTitleUCConfiguration configuration) {
    return client.getTitleCostPerUse(valueOf(id.getTitleIdPart()), valueOf(id.getPackageIdPart()), configuration);
  }

  private GetTitleUCConfiguration createConfiguration(String fiscalYear, UCSettings ucSettings, String authToken) {
    return GetTitleUCConfiguration.builder()
      .accessToken(authToken)
      .customerKey(ucSettings.getAttributes().getCustomerKey())
      .analysisCurrency(ucSettings.getAttributes().getCurrency())
      .fiscalMonth(ucSettings.getAttributes().getStartMonth().value())
      .fiscalYear(fiscalYear)
      .aggregatedFullText(true)
      .build();
  }
}
