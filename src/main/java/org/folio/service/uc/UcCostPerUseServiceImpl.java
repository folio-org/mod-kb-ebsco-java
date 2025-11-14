package org.folio.service.uc;

import static java.lang.String.valueOf;
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.parseTitleId;

import com.google.common.collect.Iterables;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.cache.VertxCache;
import org.folio.client.uc.UcApigeeEbscoClient;
import org.folio.client.uc.configuration.CommonUcConfiguration;
import org.folio.client.uc.configuration.GetPackageUcConfiguration;
import org.folio.client.uc.configuration.GetTitlePackageUcConfiguration;
import org.folio.client.uc.configuration.GetTitleUcConfiguration;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcCostAnalysisDetails;
import org.folio.client.uc.model.UcTitleCostPerUse;
import org.folio.client.uc.model.UcTitlePackageId;
import org.folio.config.cache.UcTitlePackageCacheKey;
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
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.util.template.RmApiTemplateFactory;
import org.folio.rmapi.result.PackageCostPerUseResult;
import org.folio.rmapi.result.ResourceCostPerUseCollectionResult;
import org.folio.rmapi.result.ResourceCostPerUseResult;
import org.folio.rmapi.result.TitleCostPerUseResult;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.uc.sorting.CostPerUseSort;
import org.folio.service.uc.sorting.UcSortingComparatorProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@SuppressWarnings("java:S6813")
public class UcCostPerUseServiceImpl implements UcCostPerUseService {

  public static final String INVALID_FISCAL_YEAR_MESSAGE = "Invalid fiscalYear";
  public static final String INVALID_FISCAL_YEAR_DETAILS = "Parameter 'fiscalYear' is required";
  public static final String INVALID_PLATFORM_MESSAGE = "Invalid platform";
  public static final String INVALID_PLATFORM_DETAILS =
    "Parameter 'platform' should by one of: 'all', 'publisher', 'nonPublisher'";
  public static final String INVALID_SORT_MESSAGE = "Invalid sort";
  public static final String INVALID_SORT_DETAILS =
    "Parameter 'sort' should by one of: 'name', 'type', 'cost', 'usage', 'costperuse'";
  private static final int MAX_PARTITION_SIZE = 1000;

  private static final String PARAM_VALIDATION_ERROR_MESSAGE = "validateParams:: {}: {}";

  @Autowired
  private UcAuthService authService;
  @Autowired
  @Qualifier("nonSecuredUcSettingsService")
  private UcSettingsService settingsService;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private UcApigeeEbscoClient client;
  @Autowired
  private RmApiTemplateFactory templateFactory;

  @Autowired
  private Converter<ResourceCostPerUseResult, ResourceCostPerUse> resourceCostPerUseConverter;
  @Autowired
  private Converter<ResourceCostPerUseCollectionResult, ResourceCostPerUseCollection>
    resourceCostPerUseCollectionConverter;
  @Autowired
  private Converter<PackageCostPerUseResult, PackageCostPerUse> packageCostPerUseConverter;
  @Autowired
  private Converter<TitleCostPerUseResult, TitleCostPerUse> titleCostPerUseConverter;

  @Autowired
  private VertxCache<UcTitlePackageCacheKey, Map<String, UcCostAnalysis>> ucTitlePackageCache;

  @Autowired
  private UcSortingComparatorProvider<ResourceCostPerUseCollectionItem> sortingComparatorProvider;

  @Override
  public CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform,
                                                                     String fiscalYear,
                                                                     Map<String, String> okapiHeaders) {
    log.info("getResourceCostPerUse:: Getting Resource Cost Per Use by resourceId: {}, platform: {}, fiscalYear: {}",
      resourceId, platform, fiscalYear);
    validateParams(platform, fiscalYear);
    ResourceId id = parseResourceId(resourceId);
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();
    return fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, okapiHeaders)
      .thenCompose(commonConfiguration -> {
        GetTitleUcConfiguration configuration = createGetTitleConfiguration(commonConfiguration);
        return getTitleCost(id, configuration)
          .thenApply(ucTitleCostPerUse -> ResourceCostPerUseResult.builder()
            .resourceId(id)
            .ucTitleCostPerUse(ucTitleCostPerUse)
            .configuration(configuration)
            .platformType(platformTypeHolder.get())
            .build()
          )
          .thenApply(resourceCostPerUseConverter::convert);
      });
  }

  @Override
  public CompletableFuture<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                               Map<String, String> okapiHeaders) {
    log.info("getTitleCostPerUse:: Getting Title Cost Per Use by titleId: {}, platform: {}, fiscalYear: {}",
      titleId, platform, fiscalYear);
    validateParams(platform, fiscalYear);
    return templateFactory.createTemplate(okapiHeaders, Promise.promise())
      .getRmapiTemplateContext()
      .thenCompose(context -> fetchTitleSelectedResources(titleId, context)
        .thenCompose(customerResources -> {
          if (customerResources.isEmpty()) {
            return getEmptyTitleCostPerUse(titleId);
          } else {
            return getTitleCostPerUseData(titleId, platform, fiscalYear, customerResources, context);
          }
        })
      );
  }

  @Override
  public CompletableFuture<PackageCostPerUse> getPackageCostPerUse(String packageId, String platform, String fiscalYear,
                                                                   Map<String, String> okapiHeaders) {
    log.info("getPackageCostPerUse:: Getting Package Cost Per Use by packageId: {}, platform: {}, fiscalYear: {}",
      packageId, platform, fiscalYear);
    validateParams(platform, fiscalYear);
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();

    return templateFactory.createTemplate(okapiHeaders, Promise.promise()).getRmapiTemplateContext()
      .thenCompose(context -> fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, context)
        .thenCompose(ucConfiguration ->
          composePackageCostPerUseResult(packageId, platformTypeHolder.get(), context, ucConfiguration))
      )
      .thenApply(packageCostPerUseConverter::convert);
  }

  @Override
  public CompletableFuture<ResourceCostPerUseCollection> getPackageResourcesCostPerUse(
    String packageId, String platform, String fiscalYear, String sort,
    Order order, int page, int size, Map<String, String> okapiHeaders) {
    log.info("getPackageResourcesCostPerUse:: Getting Package Resources Cost Per Use by packageId: {}, "
             + "platform: {}, fiscalYear: {}", packageId, platform, fiscalYear);
    return fetchHoldings(packageId, platform, fiscalYear, sort, okapiHeaders)
      .thenApply(resourceCostPerUseCollectionConverter::convert)
      .thenApply(collection -> createResultPage(collection, page, size, sort, order));
  }

  @Override
  public CompletableFuture<ResourceCostPerUseCollection> getPackageResourcesCostPerUse(
    String packageId, String platform, String fiscalYear, Map<String, String> okapiHeaders) {
    log.info("getPackageResourcesCostPerUse:: Getting Package Resources Cost Per Use by packageId: {}, "
             + "platform: {}, fiscalYear: {}", packageId, platform, fiscalYear);
    return fetchHoldings(packageId, platform, fiscalYear, CostPerUseSort.NAME.name(), okapiHeaders)
      .thenApply(resourceCostPerUseCollectionConverter::convert)
      .thenApply(collection -> {
        var items = collection.getData().stream()
          .sorted(sortingComparatorProvider.get(CostPerUseSort.NAME, Order.ASC))
          .toList();
        collection.withData(items);
        return collection;
      });
  }

  private CompletableFuture<ResourceCostPerUseCollectionResult> fetchHoldings(String packageId, String platform,
                                                                              String fiscalYear, String sort,
                                                                              Map<String, String> okapiHeaders) {
    log.info("fetchHoldings:: Fetching Holdings by packageId: {}, platform: {}, fiscalYear: {}",
      packageId, platform, fiscalYear);
    validateParams(platform, fiscalYear, sort);
    var id = parsePackageId(packageId);
    var packageIdPart = valueOf(id.getPackageIdPart());
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();

    return templateFactory.createTemplate(okapiHeaders, Promise.promise()).getRmapiTemplateContext()
      .thenCompose(context -> fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, context)
        .thenCompose(ucConfiguration ->
          composeResourceCostPerUseCollectionResult(packageIdPart, context, ucConfiguration,
            platformTypeHolder.get())));
  }

  private CompletableFuture<PackageCostPerUseResult> composePackageCostPerUseResult(
    String packageId, PlatformType platformType,
    RmApiTemplateContext context,
    CommonUcConfiguration ucConfiguration) {
    var packageIdPart = valueOf(parsePackageId(packageId).getPackageIdPart());
    log.info("composePackageCostPerUseResult:: Composing Result for Package Cost Per use with packageIdPart: {}, "
             + "platformType: {}", packageIdPart, platformType);
    return client.getPackageCostPerUse(packageIdPart, createGetPackageConfiguration(ucConfiguration))
      .thenCompose(ucPackageCostPerUse -> {
        var resultBuilder = PackageCostPerUseResult.builder()
          .packageId(packageId)
          .ucPackageCostPerUse(ucPackageCostPerUse)
          .configuration(ucConfiguration)
          .platformType(platformType);

        var cost = ucPackageCostPerUse.analysis().current().cost();
        if (cost == null || cost.equals(NumberUtils.DOUBLE_ZERO)) {
          return fetchHoldingsData(packageIdPart, context)
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
    String packageIdPart, RmApiTemplateContext context, CommonUcConfiguration ucConfiguration,
    PlatformType platformType) {
    var resultBuilder = ResourceCostPerUseCollectionResult.builder()
      .configuration(ucConfiguration)
      .platformType(platformType);
    return client.getPackageCostPerUse(packageIdPart, createGetPackageConfiguration(ucConfiguration))
      .thenAccept(resultBuilder::packageCostPerUse)
      .thenCompose(unused -> fetchHoldingsData(packageIdPart, context))
      .thenApply(dbHoldingInfos -> {
        resultBuilder.holdingInfos(dbHoldingInfos);
        return extractTitlePackageIds(dbHoldingInfos);
      })
      .thenCompose(ids -> fetchTitlePackageCost(ids, platformType, ucConfiguration))
      .thenApply(titlePackageCostMap -> resultBuilder.titlePackageCostMap(titlePackageCostMap).build());
  }

  private ResourceCostPerUseCollection createResultPage(ResourceCostPerUseCollection resourceCostPerUseCollection,
                                                        int page,
                                                        int size, String sort, Order order) {
    var items = resourceCostPerUseCollection.getData().stream()
      .sorted(sortingComparatorProvider.get(CostPerUseSort.from(sort), order))
      .skip((long) (page - 1) * size)
      .limit(size)
      .toList();
    return resourceCostPerUseCollection.withData(items);
  }

  private CompletableFuture<Map<String, UcCostAnalysis>> loadFromCache(List<UcTitlePackageId> titlePackageIds,
                                                                       GetTitlePackageUcConfiguration configuration) {
    @SuppressWarnings("java:S4790")
    var cacheKey = new UcTitlePackageCacheKey(configuration, DigestUtils.md5(Json.encode(titlePackageIds)));

    return ucTitlePackageCache.getValueOrLoad(cacheKey, () -> loadInPartitions(titlePackageIds, configuration));
  }

  private CompletableFuture<Map<String, UcCostAnalysis>> loadInPartitions(
    List<UcTitlePackageId> titlePackageIds,
    GetTitlePackageUcConfiguration configuration) {
    if (titlePackageIds.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptyMap());
    } else if (titlePackageIds.size() > MAX_PARTITION_SIZE) {
      var futures = StreamSupport.stream(Iterables.partition(titlePackageIds, MAX_PARTITION_SIZE).spliterator(), false)
        .map(ids -> client.getTitlePackageCostPerUse(ids, configuration))
        .toList();

      return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(unused -> futures.stream()
          .map(CompletableFuture::join)
          .flatMap(map -> map.entrySet().stream())
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    } else {
      return client.getTitlePackageCostPerUse(titlePackageIds, configuration);
    }
  }

  private CompletableFuture<List<CustomerResources>> fetchTitleSelectedResources(
    String titleId, RmApiTemplateContext rmapiTemplateContext) {
    log.info("fetchTitleSelectedResources:: Fetching Selected Title resources by titleId: {}", titleId);
    return rmapiTemplateContext.getTitlesService().retrieveTitle(parseTitleId(titleId), true)
      .thenApply(this::extractSelectedResources);
  }

  private CompletableFuture<TitleCostPerUse> getTitleCostPerUseData(String titleId, String platform, String fiscalYear,
                                                                    List<CustomerResources> customerResources,
                                                                    RmApiTemplateContext context) {
    log.info("getTitleCostPerUseData:: Getting Title cost per use data for platform: {}, titleId: {},"
             + " fiscalYear: {}, packageId: {}", platform, titleId, fiscalYear,
      customerResources.getFirst().getPackageId());
    MutableObject<PlatformType> platformTypeHolder = new MutableObject<>();
    return fetchCommonConfiguration(platform, fiscalYear, platformTypeHolder, context)
      .thenCompose(ucConfiguration -> {
        var packageId = valueOf(customerResources.getFirst().getPackageId());

        return client.getTitleCostPerUse(titleId, packageId, createGetTitleConfiguration(ucConfiguration))
          .thenCombine(fetchTitlePackagesCost(customerResources, ucConfiguration), (titleUsage, titlePackageCost) ->
            TitleCostPerUseResult.builder()
              .titleId(titleId)
              .ucTitleCostPerUse(titleUsage)
              .titlePackageCostMap(titlePackageCost)
              .customerResources(customerResources)
              .configuration(ucConfiguration)
              .platformType(platformTypeHolder.get())
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

  private CompletableFuture<Map<String, UcCostAnalysis>> fetchTitlePackagesCost(
    List<CustomerResources> customerResources,
    CommonUcConfiguration ucConfiguration) {
    var titlePackageIds = customerResources.stream()
      .map(cr -> new UcTitlePackageId(cr.getTitleId(), cr.getPackageId()))
      .distinct()
      .toList();
    log.debug("fetchTitlePackagesCost:: Fetching Title package cost with ids: {}",
      titlePackageIds.stream().map(UcTitlePackageId::toString).collect(Collectors.joining(",")));
    var configuration = createGetTitlePackageConfiguration(ucConfiguration, true);
    return loadFromCache(titlePackageIds, configuration);
  }

  private CompletableFuture<Map<String, UcCostAnalysis>> fetchTitlePackageCost(List<UcTitlePackageId> titlePackageIds,
                                                                               PlatformType platformType,
                                                                               CommonUcConfiguration ucConfiguration) {
    log.info("fetchTitlePackageCost:: Fetching Title Package Cost for platformType: {} with ids: {}",
      platformType, titlePackageIds.stream().map(UcTitlePackageId::toString).collect(Collectors.joining(",")));
    return switch (platformType) {
      case PUBLISHER -> loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, true));
      case NON_PUBLISHER -> loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, false));
      default -> loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, true))
        .thenCombine(loadFromCache(titlePackageIds, createGetTitlePackageConfiguration(ucConfiguration, false)),
          (costMap1, costMap2) -> Stream.concat(costMap1.entrySet().stream(), costMap2.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, this::toAllPublisherUcCostAnalysis)));
    };
  }

  private UcCostAnalysis toAllPublisherUcCostAnalysis(UcCostAnalysis ucCostAnalysis1, UcCostAnalysis ucCostAnalysis2) {
    var current1 = ucCostAnalysis1.current();
    var current2 = ucCostAnalysis2.current();

    var cost = Optional.ofNullable(current1.cost());
    var usage1 = Optional.ofNullable(current1.usage());
    var usage2 = Optional.ofNullable(current2.usage());

    Optional<Integer> usage = usage1.flatMap(left -> usage2.map(right -> left + right));

    var costPerUse = cost.flatMap(c -> usage.map(u -> c / u));
    log.debug("toAllPublisherUcCostAnalysis:: Uc Cost Analysis: {}, usage: {}, cost per use: {}", cost,
      usage, costPerUse);
    return new UcCostAnalysis(new UcCostAnalysisDetails(
      cost.orElse(null),
      usage.orElse(null),
      costPerUse.orElse(null)
    ), null);
  }

  private CompletableFuture<List<DbHoldingInfo>> fetchHoldingsData(String packageIdPart, RmApiTemplateContext context) {
    log.info("fetchHoldingsData:: Get Holdings data by packageId: {} ", packageIdPart);
    return holdingsService
      .getHoldingsByPackageId(packageIdPart, context.getCredentialsId(), context.getOkapiData().getTenant());
  }

  private CompletableFuture<CommonUcConfiguration> fetchCommonConfiguration(
    String platform, String fiscalYear,
    MutableObject<PlatformType> platformTypeHolder,
    Map<String, String> okapiHeaders) {
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(false, okapiHeaders),
        toCommonUcConfiguration(platform, fiscalYear, platformTypeHolder)
      );
  }

  private CompletableFuture<CommonUcConfiguration> fetchCommonConfiguration(
    String platform, String fiscalYear,
    MutableObject<PlatformType> platformTypeHolder,
    RmApiTemplateContext context) {
    Map<String, String> okapiHeaders = context.getOkapiData().getHeaders();
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByCredentialsId(context.getCredentialsId(), false, okapiHeaders),
        toCommonUcConfiguration(platform, fiscalYear, platformTypeHolder)
      );
  }

  private BiFunction<String, UCSettings, CommonUcConfiguration> toCommonUcConfiguration(
    String platform, String fiscalYear,
    MutableObject<PlatformType> platformTypeHolder) {
    return (authToken, ucSettings) -> {
      if (platform == null) {
        log.info("toCommonUcConfiguration:: Setting platformTypeHolder: {} value and building "
                 + "CommonUcConfiguration object", ucSettings.getAttributes().getPlatformType());
        platformTypeHolder.setValue(ucSettings.getAttributes().getPlatformType());
      } else {
        log.info("toCommonUcConfiguration:: Setting platformTypeHolder: {} value and building "
                 + "CommonUcConfiguration object", PlatformType.fromValue(platform));
        platformTypeHolder.setValue(PlatformType.fromValue(platform));
      }
      return createCommonConfiguration(ucSettings, fiscalYear, authToken);
    };
  }

  private List<CustomerResources> extractSelectedResources(Title title) {
    return title.getCustomerResourcesList()
      .stream()
      .filter(CustomerResources::getIsSelected)
      .toList();
  }

  private void validateParams(String platform, String fiscalYear) {
    log.debug("validateParams:: Validating request params [platform: {}, fiscalYear: {}]", platform, fiscalYear);
    if (StringUtils.isBlank(fiscalYear)) {
      log.warn(PARAM_VALIDATION_ERROR_MESSAGE, INVALID_FISCAL_YEAR_MESSAGE, INVALID_FISCAL_YEAR_DETAILS);
      throw new InputValidationException(INVALID_FISCAL_YEAR_MESSAGE, INVALID_FISCAL_YEAR_DETAILS);
    }
    if (StringUtils.isNotBlank(platform)) {
      try {
        PlatformType.fromValue(platform);
      } catch (IllegalArgumentException e) {
        log.warn(PARAM_VALIDATION_ERROR_MESSAGE, INVALID_PLATFORM_MESSAGE, INVALID_PLATFORM_DETAILS);
        throw new InputValidationException(INVALID_PLATFORM_MESSAGE, INVALID_PLATFORM_DETAILS);
      }
    }
  }

  private void validateParams(String platform, String fiscalYear, String sort) {
    validateParams(platform, fiscalYear);
    if (!CostPerUseSort.contains(sort)) {
      log.warn(PARAM_VALIDATION_ERROR_MESSAGE, INVALID_SORT_MESSAGE, INVALID_SORT_DETAILS);
      throw new InputValidationException(INVALID_SORT_MESSAGE, INVALID_SORT_DETAILS);
    }
  }

  private List<UcTitlePackageId> extractTitlePackageIds(List<DbHoldingInfo> dbHoldingInfos) {
    return dbHoldingInfos.stream()
      .map(h -> new UcTitlePackageId(h.getTitleId(), h.getPackageId()))
      .distinct()
      .toList();
  }

  private CompletableFuture<UcTitleCostPerUse> getTitleCost(ResourceId id, GetTitleUcConfiguration configuration) {
    log.info("getTitleCost:: Get by titleId: {}, packageId: {} and configurations",
      id.getTitleIdPart(), id.getPackageIdPart());
    return client.getTitleCostPerUse(valueOf(id.getTitleIdPart()), valueOf(id.getPackageIdPart()), configuration);
  }

  private CommonUcConfiguration createCommonConfiguration(UCSettings ucSettings, String fiscalYear, String authToken) {
    return CommonUcConfiguration.builder()
      .accessToken(authToken)
      .customerKey(ucSettings.getAttributes().getCustomerKey())
      .analysisCurrency(ucSettings.getAttributes().getCurrency())
      .fiscalMonth(ucSettings.getAttributes().getStartMonth().value())
      .fiscalYear(fiscalYear)
      .build();
  }

  private GetTitleUcConfiguration createGetTitleConfiguration(CommonUcConfiguration ucConfiguration) {
    return GetTitleUcConfiguration.builder()
      .accessToken(ucConfiguration.getAccessToken())
      .customerKey(ucConfiguration.getCustomerKey())
      .analysisCurrency(ucConfiguration.getAnalysisCurrency())
      .fiscalMonth(ucConfiguration.getFiscalMonth())
      .fiscalYear(ucConfiguration.getFiscalYear())
      .aggregatedFullText(true)
      .build();
  }

  private GetPackageUcConfiguration createGetPackageConfiguration(CommonUcConfiguration ucConfiguration) {
    return GetPackageUcConfiguration.builder()
      .accessToken(ucConfiguration.getAccessToken())
      .customerKey(ucConfiguration.getCustomerKey())
      .analysisCurrency(ucConfiguration.getAnalysisCurrency())
      .fiscalMonth(ucConfiguration.getFiscalMonth())
      .fiscalYear(ucConfiguration.getFiscalYear())
      .aggregatedFullText(true)
      .build();
  }

  private GetTitlePackageUcConfiguration createGetTitlePackageConfiguration(CommonUcConfiguration ucConfiguration,
                                                                            boolean isPublisher) {
    return GetTitlePackageUcConfiguration.builder()
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
