package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.repository.holdings.DbHolding;
import org.folio.repository.resources.DbResource;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceCollectionResult;

@Component
public class ResourceCollectionResultConverter implements Converter<ResourceCollectionResult, ResourceCollection> {

  @Autowired
  private Converter<Title, ResourceCollectionItem> resourceCollectionItemConverter;
  @Autowired
  private Converter<DbHolding, ResourceCollectionItem> holdingCollectionItemConverter;

  private List<String> getTagsById(List<DbResource> dbResources, ResourceId resourceId) {
    return dbResources.stream()
      .filter(dbResource -> dbResource.getId().equals(resourceId))
      .map(DbResource::getTags)
      .findFirst()
      .orElse(Collections.emptyList());
  }

  @Override
  public ResourceCollection convert(@NonNull ResourceCollectionResult resourceCollectionResult) {

    final Titles titles = resourceCollectionResult.getTitles();
    final List<DbResource> dbResources = resourceCollectionResult.getTitlesList();
    final List<DbHolding> dbHoldings = resourceCollectionResult.getHoldings();
    final List<ResourceCollectionItem> resourceCollectionItems = mapItems(titles.getTitleList(),
      title -> mapResourceCollectionItem(dbResources, resourceCollectionItemConverter.convert(title), createResourceId(title)));

    final List<ResourceCollectionItem> holdingCollectionItems = mapItems(dbHoldings,
      dbHolding -> mapResourceCollectionItem(dbResources, holdingCollectionItemConverter.convert(dbHolding), createResourceId(dbHolding)));

    resourceCollectionItems.addAll(holdingCollectionItems);
    resourceCollectionItems.sort(Comparator.comparing(o -> o.getAttributes().getName()));

    return new ResourceCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(resourceCollectionItems);
  }

  private ResourceCollectionItem mapResourceCollectionItem(List<DbResource> dbResources, ResourceCollectionItem item,
                                                           ResourceId resourceId) {
    item.getAttributes().withTags(new Tags().withTagList(getTagsById(dbResources, resourceId)));
    return item;
  }

  private ResourceId createResourceId(Title title) {
    return ResourceId.builder()
      .providerIdPart(title.getCustomerResourcesList().get(0).getVendorId())
      .packageIdPart(title.getCustomerResourcesList().get(0).getPackageId())
      .titleIdPart(title.getTitleId()).build();
  }

  private ResourceId createResourceId(DbHolding dbHolding) {
    return ResourceId.builder()
      .providerIdPart(dbHolding.getVendorId())
      .packageIdPart(Integer.parseInt(dbHolding.getPackageId()))
      .titleIdPart(Integer.parseInt(dbHolding.getTitleId())).build();
  }
}
