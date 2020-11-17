package org.folio.rest.converter.holdings;

import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;

@Component
public class HoldingCollectionItemConverter implements Converter<DbHoldingInfo, ResourceCollectionItem> {

  @Override
  public ResourceCollectionItem convert(@NonNull DbHoldingInfo holding) {

    return new ResourceCollectionItem()
      .withId(holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId())
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withRelationships(createEmptyRelationship())
      .withAttributes(new ResourceDataAttributes()
        .withTitleId(holding.getTitleId())
        .withName(holding.getPublicationTitle())
        .withPublisherName(holding.getPublisherName())
        .withPublicationType(ConverterConsts.publicationTypes.get(holding.getResourceType().toLowerCase()))
      );
  }
}
