package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Resources;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.resources.DbResource;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceCollectionResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceCollectionResultConverter implements Converter<ResourceCollectionResult, ResourceCollection> {

  private final Converter<Resource, ResourceCollectionItem> resourceCollectionItemConverter;
  private final Converter<DbHoldingInfo, ResourceCollectionItem> holdingCollectionItemConverter;

  public ResourceCollectionResultConverter(
    Converter<Resource, ResourceCollectionItem> resourceCollectionItemConverter,
    Converter<DbHoldingInfo, ResourceCollectionItem> holdingCollectionItemConverter) {
    this.resourceCollectionItemConverter = resourceCollectionItemConverter;
    this.holdingCollectionItemConverter = holdingCollectionItemConverter;
  }

  @Override
  public ResourceCollection convert(ResourceCollectionResult resourceCollectionResult) {

    final Resources resources = resourceCollectionResult.resources();
    final List<DbResource> dbResources = resourceCollectionResult.dbResources();
    final List<DbHoldingInfo> holdings = resourceCollectionResult.holdings();
    final List<ResourceCollectionItem> resourceCollectionItems = mapItems(resources.getTitles(),
      resource -> mapResourceCollectionItem(dbResources, resourceCollectionItemConverter.convert(resource),
        createResourceId(resource)));

    final List<ResourceCollectionItem> holdingCollectionItems = mapItems(holdings,
      dbHolding -> mapResourceCollectionItem(dbResources, holdingCollectionItemConverter.convert(dbHolding),
        createResourceId(dbHolding)));

    resourceCollectionItems.addAll(holdingCollectionItems);
    resourceCollectionItems.sort(Comparator.comparing(o -> o.getAttributes().getName()));

    return new ResourceCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(resources.getTotalResults()))
      .withData(resourceCollectionItems);
  }

  private List<String> getTagsById(List<DbResource> resources, ResourceId resourceId) {
    return resources.stream()
      .filter(dbResource -> dbResource.getId().equals(resourceId))
      .map(DbResource::getTags)
      .findFirst()
      .orElse(Collections.emptyList());
  }

  private ResourceCollectionItem mapResourceCollectionItem(List<DbResource> resources, ResourceCollectionItem item,
                                                           ResourceId resourceId) {
    item.getAttributes().withTags(new Tags().withTagList(getTagsById(resources, resourceId)));
    return item;
  }

  private ResourceId createResourceId(Resource resource) {
    return new ResourceId(resource.getVendorId(), resource.getPackageId(), resource.getTitleId());
  }

  private ResourceId createResourceId(DbHoldingInfo holding) {
    return new ResourceId(holding.getVendorId(), holding.getPackageId(), holding.getTitleId());
  }
}
