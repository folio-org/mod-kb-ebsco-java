package org.folio.rest.converter.resources;

import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;

@Component
public class ResourceCollectionItemConverter implements Converter<Title, ResourceCollectionItem> {

  @Autowired
  private CommonResourceConverter commonResourceConverter;

  @Override
  public ResourceCollectionItem convert(@NonNull Title title) {
    CustomerResources resource = title.getCustomerResourcesList().get(0);
    return new ResourceCollectionItem()
      .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withRelationships(createEmptyRelationship())
      .withAttributes(commonResourceConverter.createResourceDataAttributes(title, resource));
  }
}
