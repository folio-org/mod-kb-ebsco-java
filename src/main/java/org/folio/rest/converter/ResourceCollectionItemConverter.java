package org.folio.rest.converter;

import static org.folio.rest.converter.ResourcesConverter.createEmptyRelationship;

import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceCollectionItemConverter implements Converter<Title, ResourceCollectionItem> {

  @Autowired
  private CommonAttributesConverter commonConverter;
  @Autowired
  private CommonResourceConverter commonResourceConverter;

  @Override
  public ResourceCollectionItem convert(Title title) {
    CustomerResources resource = title.getCustomerResourcesList().get(0);
    return new ResourceCollectionItem()
      .withId(String.valueOf(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId()))
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withRelationships(createEmptyRelationship())
      .withAttributes(commonResourceConverter.createResourceDataAttributes(title, resource));
  }
}
