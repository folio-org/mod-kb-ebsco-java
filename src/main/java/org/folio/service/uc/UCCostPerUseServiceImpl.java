package org.folio.service.uc;

import static java.lang.String.valueOf;

import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.parseTitleId;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import org.folio.cache.VertxCache;
import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.configuration.GetPackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitlePackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCCostAnalysisDetails;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.client.uc.model.UCTitlePackageId;
import org.folio.config.cache.UCTitlePackageCacheKey;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Order;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rmapi.result.PackageCostPerUseResult;
import org.folio.rmapi.result.ResourceCostPerUseCollectionResult;
import org.folio.rmapi.result.ResourceCostPerUseResult;
import org.folio.rmapi.result.TitleCostPerUseResult;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.uc.sorting.CostPerUseSort;
import org.folio.service.uc.sorting.UCSortingComparatorProvider;

@Service
public class UCCostPerUseServiceImpl implements UCCostPerUseService {

  public static final String INVALID_FISCAL_YEAR_MESSAGE = "Invalid fiscalYear";
  public static final String INVALID_FISCAL_YEAR_DETAILS = "Parameter 'fiscalYear' is required";
  public static final String INVALID_PLATFORM_MESSAGE = "Invalid platform";
  public static final String INVALID_PLATFORM_DETAILS =
    "Parameter 'platform' should by one of: 'all', 'publisher', 'nonPublisher'";
  public static final String INVALID_SORT_MESSAGE = "Invalid sort";
  public static final String INVALID_SORT_DETAILS =
    "Parameter 'sort' should by one of: 'name', 'type', 'cost', 'usage', 'costperuse'";
  private static final int MAX_PARTITION_SIZE = 1000;

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
  private Converter<ResourceCostPerUseCollectionResult, ResourceCostPerUseCollection> resourceCostPerUseCollectionConverter;
  @Autowired
  private Converter<PackageCostPerUseResult, PackageCostPerUse> packageCostPerUseConverter;
  @Autowired
  private Converter<TitleCostPerUseResult, TitleCostPerUse> titleCostPerUseConverter;

  @Autowired
  private VertxCache<UCTitlePackageCacheKey, Map<String, UCCostAnalysis>> ucTitlePackageCache;

  @Autowired
  private UCSortingComparatorProvider<ResourceCostPerUseCollectionItem> sortingComparatorProvider;

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
      .thenCompose(context -> fetchTitleSelectedResources(titleId, context)
        .thenCompose(customerResources -> {
          if (customerResources.isEmpty()) {
            return getEmptyTitleCostPerUse(titleId);
          } else {
            return getTitleCostPerUse(titleId, platform, fiscalYear, customerResources, context);
          }
        })
      );
  }

  @Override
  public CompletableFuture<PackageCostPerUse> getPackageCostPerUse(String packageId, String platform, String fiscalYear,
                                                                   Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear);
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();

    return templateFactory.createTemplate(okapiHeaders, Promise.promise()).getRmapiTemplateContext()
      .thenCompose(context -> fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, context)
        .thenCompose(ucConfiguration ->
          composePackageCostPerUseResult(packageId, platformTypeHolder.getValue(), context, ucConfiguration))
      )
      .thenApply(packageCostPerUseConverter::convert);
  }

  @Override
  public CompletableFuture<ResourceCostPerUseCollection> getPackageResourcesCostPerUse(String packageId, String platform,
                                                                                       String fiscalYear, String sort,
                                                                                       Order order, int page, int size,
                                                                                       Map<String, String> okapiHeaders) {

    return fetchHoldings(packageId, platform, fiscalYear, sort, okapiHeaders)
      .thenApply(resourceCostPerUseCollectionConverter::convert)
      .thenApply(collection -> createResultPage(collection, page, size, sort, order));
  }

  private CompletableFuture<ResourceCostPerUseCollectionResult> fetchHoldings(String packageId, String platform,
                                                                              String fiscalYear, String sort,
                                                                              Map<String, String> okapiHeaders) {
    validateParams(platform, fiscalYear, sort);
    var id = parsePackageId(packageId);
    var packageIdPart = valueOf(id.getPackageIdPart());
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();

    return templateFactory.createTemplate(okapiHeaders, Promise.promise()).getRmapiTemplateContext()
      .thenCompose(context -> fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, context)
        .thenCompose(ucConfiguration ->
          composeResourceCostPerUseCollectionResult(packageIdPart, context, ucConfiguration, platformTypeHolder.getValue())));
  }

  @Override
  public CompletableFuture<ResourceCostPerUseCollection> getPackageResourcesCostPerUse(String packageId, String platform, String fiscalYear, Map<String, String> okapiHeaders) {
    return fetchHoldings(packageId, platform, fiscalYear, CostPerUseSort.NAME.name(), okapiHeaders)
      .thenApply(resourceCostPerUseCollectionConverter::convert)
      .thenApply(collection -> {
        var items = collection.getData().stream()
          .sorted(sortingComparatorProvider.get(CostPerUseSort.NAME, Order.ASC))
          .collect(Collectors.toList());
        collection.withData(items);
        return collection;
      });
  }

  private CompletableFuture<PackageCostPerUseResult> composePackageCostPerUseResult(String packageId,
                                                                                    PlatformType platformType,
                                                                                    RMAPITemplateContext context,
                                                                                    CommonUCConfiguration ucConfiguration) {
    var packageIdPart = valueOf(parsePackageId(packageId).getPackageIdPart());
    return client.getPackageCostPerUse(packageIdPart, createGetPackageConfiguration(ucConfiguration))
      .thenCompose(ucPackageCostPerUse -> {
        var resultBuilder = PackageCostPerUseResult.builder()
          .packageId(packageId)
          .ucPackageCostPerUse(ucPackageCostPerUse)
          .configuration(ucConfiguration)
          .platformType(platformType);

        var cost = ucPackageCostPerUse.getAnalysis().getCurrent().getCost();
        if (cost == null || cost.equals(NumberUtils.DOUBLE_ZERO)) {
          return fetchHoldings(packageIdPart, context)
            .thenCompose(dbHoldingInfos -> {
              var configuration = createGetTitlePackageConfiguration(ucConfiguration, true);
              return loadFromCache(extractTitlePackageIds(dbHoldingInfos), configuration);
            })
            .thenApply(titlePackageCost -> resultBuilder.titlePackageCostMap(titlePackageCost).build());
        } else {
          return CompletableFuture.completedFuture(resultBuilder.build());
        }
      });
  }

  private CompletableFuture<ResourceCostPerUseCollectionResult> composeResourceCostPerUseCollectionResult(
    String packageIdPart, RMAPITemplateContext context, CommonUCConfiguration ucConfiguration, PlatformType platformType) {
    var resultBuilder = ResourceCostPerUseCollectionResult.builder().configuration(ucConfiguration);
    return client.getPackageCostPerUse(packageIdPart, createGetPackageConfiguration(ucConfiguration))
      .thenAccept(resultBuilder::packageCostPerUse)
      .thenCompose(unused -> fetchHoldings(packageIdPart, context))
      .thenApply(dbHoldingInfos -> {
        resultBuilder.holdingInfos(dbHoldingInfos);
        return extractTitlePackageIds(dbHoldingInfos);
      })
      .thenCompose(ids -> fetchTitlePackageCost(ids, platformType, ucConfiguration))
      .thenApply(titlePackageCostMap -> resultBuilder.titlePackageCostMap(titlePackageCostMap).build());
  }

  private ResourceCostPerUseCollection createResultPage(ResourceCostPerUseCollection resourceCostPerUseCollection, int page,
                                                        int size, String sort, Order order) {
    var items = resourceCostPerUseCollection.getData().stream()
      .sorted(sortingComparatorProvider.get(CostPerUseSort.from(sort), order))
      .skip((long) (page - 1) * size)
      .limit(size)
      .collect(Collectors.toList());
    return resourceCostPerUseCollection.withData(items);
  }

  private CompletableFuture<Map<String, UCCostAnalysis>> loadFromCache(List<UCTitlePackageId> titlePackageIds,
                                                                       GetTitlePackageUCConfiguration configuration) {
    var cacheKey = new UCTitlePackageCacheKey(configuration, DigestUtils.md5(Json.encode(titlePackageIds)));

    return ucTitlePackageCache.getValueOrLoad(cacheKey, () -> loadInPartitions(titlePackageIds, configuration));
  }

  private CompletableFuture<Map<String, UCCostAnalysis>> loadInPartitions(List<UCTitlePackageId> titlePackageIds,
                                                                          GetTitlePackageUCConfiguration configuration) {
    if (titlePackageIds.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptyMap());
    } else if (titlePackageIds.size() > MAX_PARTITION_SIZE) {
      var futures = StreamSupport.stream(Iterables.partition(titlePackageIds, MAX_PARTITION_SIZE).spliterator(), false)
        .map(ids -> client.getTitlePackageCostPerUse(ids, configuration))
        .collect(Collectors.toList());

      return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(unused -> futures.stream()
          .map(CompletableFuture::join)
          .flatMap(map -> map.entrySet().stream())
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    } else {
      return client.getTitlePackageCostPerUse(titlePackageIds, configuration);
    }
  }

  private CompletableFuture<List<CustomerResources>> fetchTitleSelectedResources(String titleId,
                                                                                 RMAPITemplateContext rmapiTemplateContext) {
    return rmapiTemplateContext.getTitlesService().retrieveTitle(parseTitleId(titleId), true)
      .thenApply(this::extractSelectedResources);
  }

  private CompletableFuture<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                                List<CustomerResources> customerResources,
                                                                RMAPITemplateContext context) {
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();
    return fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, context)
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
    var titlePackageIds = customerResources.stream()
      .map(cr -> new UCTitlePackageId(cr.getTitleId(), cr.getPackageId()))
      .distinct()
      .collect(Collectors.toList());
    var configuration = createGetTitlePackageConfiguration(ucConfiguration, true);
    return loadFromCache(titlePackageIds, configuration);
  }

  private CompletableFuture<Map<String, UCCostAnalysis>> fetchTitlePackageCost(List<UCTitlePackageId> titlePackageIds,
                                                                               PlatformType platformType,
                                                                               CommonUCConfiguration ucConfiguration) {
    switch (platformType) {
      case PUBLISHER:
        return loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, true));
      case NON_PUBLISHER:
        return loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, false));
      default:
        return loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, true))
          .thenCombine(loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, false)),
            (costMap1, costMap2) -> Stream.concat(costMap1.entrySet().stream(), costMap2.entrySet().stream())
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, this::toAllPublisherUCCostAnalysis)));
    }
  }

  private UCCostAnalysis toAllPublisherUCCostAnalysis(UCCostAnalysis ucCostAnalysis1, UCCostAnalysis ucCostAnalysis2) {
    var current1 = ucCostAnalysis1.getCurrent();
    var current2 = ucCostAnalysis2.getCurrent();

    var cost = Optional.ofNullable(current1.getCost());
    var usage1 = Optional.ofNullable(current1.getUsage());
    var usage2 = Optional.ofNullable(current2.getUsage());

    Optional<Integer> usage = usage1.flatMap(left -> usage2.map(right -> left + right));

    var costPerUse = cost.flatMap(c -> usage.map(u -> c / u));
    return new UCCostAnalysis(new UCCostAnalysisDetails(
      cost.orElse(null),
      usage.orElse(null),
      costPerUse.orElse(null)
    ), null);
  }

  private CompletableFuture<List<DbHoldingInfo>> fetchHoldings(String packageIdPart, RMAPITemplateContext context) {
    return holdingsService
      .getHoldingsByPackageId(packageIdPart, context.getCredentialsId(), context.getOkapiData().getTenant());
  }

  private CompletableFuture<CommonUCConfiguration> fetchCommonConfiguration(String platform, String fiscalYear,
                                                                            MutableObject<PlatformType> platformTypeHolder,
                                                                            Map<String, String> okapiHeaders) {
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(okapiHeaders),
        toCommonUCConfiguration(platform, fiscalYear, platformTypeHolder)
      );
  }

  private CompletableFuture<CommonUCConfiguration> fetchCommonConfiguration(String platform, String fiscalYear,
                                                                            MutableObject<PlatformType> platformTypeHolder,
                                                                            RMAPITemplateContext context) {
    Map<String, String> okapiHeaders = context.getOkapiData().getOkapiHeaders();
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByCredentialsId(context.getCredentialsId(), okapiHeaders),
        toCommonUCConfiguration(platform, fiscalYear, platformTypeHolder)
      );
  }

  private BiFunction<String, UCSettings, CommonUCConfiguration> toCommonUCConfiguration(String platform, String fiscalYear,
                                                                                        MutableObject<PlatformType> platformTypeHolder) {
    return (authToken, ucSettings) -> {
      if (platform == null) {
        platformTypeHolder.setValue(ucSettings.getAttributes().getPlatformType());
      } else {
        platformTypeHolder.setValue(PlatformType.fromValue(platform));
      }
      return createCommonConfiguration(ucSettings, fiscalYear, authToken);
    };
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

  private void validateParams(String platform, String fiscalYear, String sort) {
    validateParams(platform, fiscalYear);
    if (!CostPerUseSort.contains(sort)) {
      throw new InputValidationException(INVALID_SORT_MESSAGE, INVALID_SORT_DETAILS);
    }
  }

  private List<UCTitlePackageId> extractTitlePackageIds(List<DbHoldingInfo> dbHoldingInfos) {
    return dbHoldingInfos.stream()
      .map(h -> new UCTitlePackageId(h.getTitleId(), h.getPackageId()))
      .distinct()
      .collect(Collectors.toList());
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

  private GetTitlePackageUCConfiguration createGetTitlePackageConfiguration(CommonUCConfiguration ucConfiguration,
                                                                            boolean isPublisher) {
    return GetTitlePackageUCConfiguration.builder()
      .accessToken(ucConfiguration.getAccessToken())
      .customerKey(ucConfiguration.getCustomerKey())
      .analysisCurrency(ucConfiguration.getAnalysisCurrency())
      .fiscalMonth(ucConfiguration.getFiscalMonth())
      .fiscalYear(ucConfiguration.getFiscalYear())
      .publisherPlatform(isPublisher)
      .previousYear(false)
      .build();
  }

}
