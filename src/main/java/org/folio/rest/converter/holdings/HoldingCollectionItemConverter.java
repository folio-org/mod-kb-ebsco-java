package org.folio.rest.converter.holdings;

import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class HoldingCollectionItemConverter
  implements Converter<@NonNull DbHoldingInfo, @NonNull ResourceCollectionItem> {

  @Override
  public ResourceCollectionItem convert(DbHoldingInfo holding) {

    return new ResourceCollectionItem()
      .withId(holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId())
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withRelationships(createEmptyRelationship())
      .withAttributes(new ResourceDataAttributes()
        .withTitleId(holding.getTitleId())
        .withName(holding.getPublicationTitle())
        .withPublisherName(holding.getPublisherName())
        .withPublicationType(ConverterConsts.PUBLICATION_TYPES.get(holding.getResourceType().toLowerCase()))
      );
  }
}
