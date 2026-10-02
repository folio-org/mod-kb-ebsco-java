package org.folio.rest.impl;

import static org.folio.common.ListUtils.parseByComma;
import static org.folio.rest.util.ExceptionMappers.error400NotFoundMapper;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.RestConstants.JSONAPI;
import static org.folio.rest.util.RestConstants.TAGS_TYPE;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.RequestContext;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageBulkFetchCollection;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackagePostBulkFetchRequest;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsItem;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.resource.EholdingsPackages;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.model.filter.PackageRecordFilter;
import org.folio.rest.model.filter.ResourceFilter;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RmApiTemplate;
import org.folio.rest.util.template.RmApiTemplateFactory;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.packages.PackageService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("java:S6813")
public class EholdingsPackagesImpl implements EholdingsPackages {

  private static final String PACKAGE_NOT_FOUND_MESSAGE = "Package not found";

  @Autowired
  private PackageService packageService;
  @Autowired
  private RmApiTemplateFactory templateFactory;
  @Autowired
  private FilteredEntitiesLoader filteredEntitiesLoader;
  @Autowired
  @Qualifier("securedUserCredentialsService")
  private UserKbCredentialsService userKbCredentialsService;

  public EholdingsPackagesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @SuppressWarnings("checkstyle:MethodLength")
  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackages(String filterCustom, String q, String queryField, String queryType,
                                   boolean highlight, String filterSelected, String filterType, String filterVisibility,
                                   String filterFreeAccess, List<String> filterTags, List<String> filterAccessType,
                                   String sort, int page, int count, Map<String, String> okapiHeaders,
                                   Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    var filter = PackageRecordFilter.builder()
      .query(q)
      .queryField(queryField)
      .queryType(queryType)
      .highlight(highlight)
      .filterCustom(filterCustom)
      .filterSelected(filterSelected)
      .filterType(filterType)
      .filterVisibility(filterVisibility)
      .filterFreeAccess(filterFreeAccess)
      .filterTags(filterTags)
      .filterAccessType(filterAccessType)
      .sort(sort)
      .page(page)
      .count(count)
      .build();

    var template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    if (filter.isTagsFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchPackagesByTagFilter(TagFilter.from(filter), context));
    } else if (filter.isAccessTypeFilter()) {
      template.requestAction(context -> filteredEntitiesLoader
        .fetchPackagesByAccessTypeFilter(AccessTypeFilter.from(filter), context));
    } else {
      template.requestAction(context -> {
        if (Boolean.TRUE.equals(filter.resolveFilterCustom())) {
          return packageService.getCustomProviderIdAndRetrievePackages(filter, context);
        } else {
          return packageService.retrievePackages(null, filter, context);
        }
      });
    }

    template.executeWithResult(PackageCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsPackages(String contentType, PackagePostRequest entity, Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    template.requestAction(context -> packageService.createCustomPackage(entity, context));

    template
      .addErrorMapper(NotFoundException.class, error400NotFoundMapper())
      .addErrorMapper(InputValidationException.class, error422InputValidationMapper())
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsPackagesByPackageId(String packageId, String include, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    List<String> includedObjects = parseByComma(include);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> packageService.retrievePackageWithRelatedData(parsedPackageId, includedObjects,
        context))
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsPackagesByPackageId(String packageId, String contentType, PackagePutRequest entity,
                                              Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> packageService.updatePackage(parsedPackageId, entity, context))
      .addErrorMapper(NotFoundException.class, error400NotFoundMapper())
      .addErrorMapper(InputValidationException.class, error422InputValidationMapper())
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsPackagesByPackageId(String packageId, Map<String, String> okapiHeaders,
                                                 Handler<AsyncResult<Response>> asyncResultHandler,
                                                 Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> packageService.deletePackage(parsedPackageId, context))
      .execute();
  }

  @Override
  @Validate
  @HandleValidationErrors
  @SuppressWarnings("checkstyle:MethodLength")
  public void getEholdingsPackagesResourcesByPackageId(String packageId, List<String> filterTags,
                                                       List<String> filterAccessType, String filterSelected,
                                                       String filterType, String filterName, String filterIsxn,
                                                       String filterSubject, String filterPublisher, String sort,
                                                       int page,
                                                       int count, Map<String, String> okapiHeaders,
                                                       Handler<AsyncResult<Response>> asyncResultHandler,
                                                       Context vertxContext) {

    var filter = ResourceFilter.builder()
      .packageId(packageId)
      .filterTags(filterTags)
      .filterAccessType(filterAccessType)
      .filterSelected(filterSelected)
      .filterType(filterType)
      .filterName(filterName)
      .filterIsxn(filterIsxn)
      .filterSubject(filterSubject)
      .filterPublisher(filterPublisher)
      .sort(sort)
      .page(page)
      .count(count)
      .build();

    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if (filter.isTagsFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchResourcesByTagFilter(TagFilter.from(filter), context));
    } else if (filter.isAccessTypeFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchResourcesByAccessTypeFilter(AccessTypeFilter.from(filter), context));
    } else {
      template.requestAction(packageService.retrievePackageTitles(filter));
    }

    template.addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsPackagesResourcesByPackageIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(PACKAGE_NOT_FOUND_MESSAGE)))
      .executeWithResult(ResourceCollection.class);
  }

  @Override
  public void putEholdingsPackagesTagsByPackageId(String packageId, String contentType, PackageTagsPutRequest entity,
                                                  Map<String, String> headers,
                                                  Handler<AsyncResult<Response>> asyncResultHandler,
                                                  Context vertxContext) {
    userKbCredentialsService.findByUser(headers)
      .thenCompose(creds -> packageService.updateTagsForPackage(entity, UUID.fromString(creds.getId()), packageId,
          new RequestContext(headers).getTenant())
        .thenAccept(attributes ->
          asyncResultHandler
            .handle(
              Future.succeededFuture(PutEholdingsPackagesTagsByPackageIdResponse.respond200WithApplicationVndApiJson(
                convertToPackageTags(attributes))))))
      .exceptionally(e -> {
        new ErrorHandler()
          .addInputValidation422Mapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Validate
  @Override
  public void postEholdingsPackagesBulkFetch(String contentType, PackagePostBulkFetchRequest entity,
                                             Map<String, String> okapiHeaders,
                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    final RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    template.requestAction(context -> context.getPackagesService().retrievePackagesBulk(entity.getPackages()))
      .executeWithResult(PackageBulkFetchCollection.class);
  }

  private PackageTags convertToPackageTags(PackageTagsDataAttributes attributes) {
    return new PackageTags()
      .withData(new PackageTagsItem()
        .withType(TAGS_TYPE)
        .withAttributes(attributes))
      .withJsonapi(JSONAPI);
  }
}
