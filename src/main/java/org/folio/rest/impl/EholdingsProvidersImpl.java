package org.folio.rest.impl;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.IdParser.getPackageIds;
import static org.folio.rest.util.IdParser.parseProviderId;
import static org.folio.rest.util.RestConstants.JSONAPI;
import static org.folio.rest.util.RestConstants.TAGS_TYPE;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.model.VendorPut;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.providers.DbProvider;
import org.folio.repository.providers.ProviderRepository;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderPutDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.ProviderTags;
import org.folio.rest.jaxrs.model.ProviderTagsDataAttributes;
import org.folio.rest.jaxrs.model.ProviderTagsItem;
import org.folio.rest.jaxrs.model.ProviderTagsPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.model.filter.Filter;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RmApiTemplate;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.util.template.RmApiTemplateFactory;
import org.folio.rest.validator.ProviderPutBodyValidator;
import org.folio.rest.validator.ProviderTagsPutBodyValidator;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.VendorResult;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;

@SuppressWarnings("java:S6813")
public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";

  @Autowired
  private Converter<ProviderPutRequest, VendorPut> putRequestConverter;
  @Autowired
  private ProviderPutBodyValidator bodyValidator;
  @Autowired
  private ProviderTagsPutBodyValidator providerTagsPutBodyValidator;
  @Autowired
  private RmApiTemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private ProviderRepository providerRepository;
  @Autowired
  private PackageRepository packageRepository;
  @Autowired
  private RelatedEntitiesLoader relatedEntitiesLoader;
  @Autowired
  private FilteredEntitiesLoader filteredEntitiesLoader;
  @Autowired
  @Qualifier("securedUserCredentialsService")
  private UserKbCredentialsService userKbCredentialsService;
  @Value("${kb.ebsco.search-type.packages}")
  private String packagesSearchType;

  public EholdingsProvidersImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, List<String> filterTags, String sort, int page, int count,
                                    Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    Filter filter = Filter.builder()
      .recordType(RecordType.PROVIDER)
      .query(q)
      .filterTags(filterTags)
      .sort(sort)
      .page(page)
      .count(count)
      .build();
    if (filter.isTagsFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchProvidersByTagFilter(filter.createTagFilter(), context));
    } else {
      template
        .requestAction(context ->
          context.getProvidersService().retrieveProviders(q, page, count, filter.getSort()));
    }
    template.executeWithResult(ProviderCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersByProviderId(String providerId, String include, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler,
                                                Context vertxContext) {
    long providerIdLong = parseProviderId(providerId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getProvidersService().retrieveProvider(providerIdLong, include)
          .thenCompose(result -> loadTags(result, context))
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsProvidersByProviderIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)))
      .executeWithResult(Provider.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsProvidersByProviderId(String providerId, String contentType, ProviderPutRequest entity,
                                                Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler,
                                                Context vertxContext) {
    long providerIdLong = parseProviderId(providerId);

    bodyValidator.validate(entity);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        processUpdateRequest(entity, providerIdLong, context)
          .thenApply(result -> new VendorResult(result, null)))
      .addErrorMapper(InputValidationException.class, error422InputValidationMapper())
      .executeWithResult(Provider.class);
  }

  @Override
  public void putEholdingsProvidersTagsByProviderId(String providerId, String contentType,
                                                    ProviderTagsPutRequest entity,
                                                    Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    final Tags tags = entity.getData().getAttributes().getTags();

    userKbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(creds -> {
        ProviderTagsDataAttributes attributes = entity.getData().getAttributes();
        providerTagsPutBodyValidator.validate(entity, attributes);
        return updateTags(
          createDbProvider(providerId, UUID.fromString(creds.getId()), entity.getData().getAttributes()),
          tags, new OkapiData(okapiHeaders).getTenant())
          .thenAccept(ob -> asyncResultHandler.handle(
            Future.succeededFuture(PutEholdingsProvidersTagsByProviderIdResponse.respond200WithApplicationVndApiJson(
              convertToProviderTags(attributes)
            ))));
      })
      .exceptionally(e -> {
        new ErrorHandler()
          .addInputValidation422Mapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProvidersPackagesByProviderId(String providerId, String q, List<String> filterTags,
                                                        List<String> filterAccessType, String filterSelected,
                                                        String filterType, String sort, int page, int count,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    Filter filter = Filter.builder()
      .recordType(RecordType.PACKAGE)
      .query(q)
      .filterTags(filterTags)
      .providerId(providerId)
      .filterAccessType(filterAccessType)
      .filterSelected(filterSelected)
      .filterType(filterType)
      .sort(sort)
      .page(page)
      .count(count)
      .build();

    if (filter.isTagsFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchPackagesByTagFilter(filter.createTagFilter(), context));
    } else if (filter.isAccessTypeFilter()) {
      template.requestAction(context -> filteredEntitiesLoader
        .fetchPackagesByAccessTypeFilter(filter.createAccessTypeFilter(), context)
        .thenApply(packages -> new PackageCollectionResult(packages, emptyList()))
      );
    } else {
      template
        .requestAction(context ->
          context.getPackagesService()
            .retrievePackages(filter.getFilterSelected(), filterType, packagesSearchType, filter.getProviderId(), q,
              page,
              count, filter.getSort())
            .thenCompose(packages -> loadTags(packages, context)));
    }
    template
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsProvidersPackagesByProviderIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)
        ))
      .executeWithResult(PackageCollection.class);
  }

  private CompletableFuture<VendorResult> loadTags(VendorResult result, RmApiTemplateContext context) {
    RecordKey recordKey = RecordKey.builder()
      .recordId(String.valueOf(result.getVendor().getVendorId()))
      .recordType(RecordType.PROVIDER)
      .build();
    return relatedEntitiesLoader.loadTags(result, recordKey, context).thenApply(v -> result);
  }

  private CompletableFuture<PackageCollectionResult> loadTags(Packages packages, RmApiTemplateContext context) {
    UUID credentialsId = toUUID(context.getCredentialsId());
    String tenant = context.getOkapiData().getTenant();
    return packageRepository.findByIds(getPackageIds(packages), credentialsId, tenant)
      .thenApply(dbPackages -> new PackageCollectionResult(packages, dbPackages));
  }

  private CompletableFuture<Void> updateTags(DbProvider provider, Tags tags, String tenant) {
    if (Objects.isNull(tags)) {
      return completedFuture(null);
    } else {
      return updateStoredProvider(provider, tags, tenant)
        .thenCompose(
          o -> tagRepository.updateRecordTags(tenant, provider.getId(), RecordType.PROVIDER, tags.getTagList()))
        .thenApply(updated -> null);
    }
  }

  private ProviderTags convertToProviderTags(ProviderTagsDataAttributes attributes) {
    return new ProviderTags()
      .withData(new ProviderTagsItem()
        .withType(TAGS_TYPE)
        .withAttributes(attributes))
      .withJsonapi(JSONAPI);
  }

  private DbProvider createDbProvider(String providerId, UUID credentialsId,
                                      ProviderTagsDataAttributes attributes) {
    return DbProvider.builder()
      .id(providerId)
      .credentialsId(credentialsId)
      .name(attributes.getName())
      .build();
  }

  private CompletableFuture<Void> updateStoredProvider(DbProvider provider, Tags tags, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return providerRepository.save(provider, tenant);
    }
    return providerRepository.delete(provider.getId(), provider.getCredentialsId(), tenant);
  }

  private CompletableFuture<VendorById> processUpdateRequest(ProviderPutRequest request, long providerIdLong,
                                                             RmApiTemplateContext context) {
    if (!providerCanBeUpdated(request)) {
      //Return current state of provider without updating it
      return context.getProvidersService().retrieveProvider(providerIdLong);
    }
    return context.getProvidersService().updateProvider(providerIdLong, putRequestConverter.convert(request));
  }

  private boolean providerCanBeUpdated(ProviderPutRequest request) {
    ProviderPutDataAttributes attributes = request.getData().getAttributes();
    return !Objects.isNull(attributes.getPackagesSelected()) && attributes.getPackagesSelected() != 0;
  }
}
