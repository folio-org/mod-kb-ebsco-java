package org.folio.rest.converter.holdings;

import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.repository.holdings.DbHolding;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;

@Component
public class HoldingCollectionItemConverter implements Converter<DbHolding, ResourceCollectionItem> {

  @Override
  public ResourceCollectionItem convert(@NonNull DbHolding dbHolding) {

    return new ResourceCollectionItem()
      .withId(dbHolding.getVendorId() + "-" + dbHolding.getPackageId() + "-" + dbHolding.getTitleId())
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withRelationships(createEmptyRelationship())
      .withAttributes(new ResourceDataAttributes()
        .withTitleId(Integer.parseInt(dbHolding.getTitleId()))
        .withName(dbHolding.getPublicationTitle())
        .withPublisherName(dbHolding.getPublisherName())
        .withPublicationType(ConverterConsts.publicationTypes.get(dbHolding.getResourceType().toLowerCase()))
      );
  }
}
