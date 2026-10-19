package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import java.util.List;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rmapi.ResourceMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitleConverter implements Converter<Title, List<ResourceCollectionItem>> {

  private final CommonResourceConverter commonResourceConverter;

  public TitleConverter(CommonResourceConverter commonResourceConverter) {
    this.commonResourceConverter = commonResourceConverter;
  }

  @Override
  public List<ResourceCollectionItem> convert(Title rmapiTitle) {
    return mapItems(rmapiTitle.getCustomerResourcesList(),
      resource -> new ResourceCollectionItem()
        .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withAttributes(commonResourceConverter.createResourceDataAttributes(
          ResourceMapper.toResource(rmapiTitle, resource)))
        .withRelationships(createEmptyRelationship()));
  }
}
