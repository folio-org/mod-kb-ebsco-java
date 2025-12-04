package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import java.util.List;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitleConverter implements Converter<org.folio.holdingsiq.model.Title, List<ResourceCollectionItem>> {

  private final CommonResourceConverter commonResourceConverter;

  public TitleConverter(CommonResourceConverter commonResourceConverter) {
    this.commonResourceConverter = commonResourceConverter;
  }

  @Override
  public List<@NonNull ResourceCollectionItem> convert(org.folio.holdingsiq.model.Title rmapiTitle) {
    List<CustomerResources> customerResourcesList = rmapiTitle.getCustomerResourcesList();
    return mapItems(customerResourcesList,
      resource -> new ResourceCollectionItem()
        .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withAttributes(commonResourceConverter.createResourceDataAttributes(rmapiTitle, resource))
        .withRelationships(createEmptyRelationship()));
  }
}
