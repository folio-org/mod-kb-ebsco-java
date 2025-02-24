package org.folio.rest.converter.resources;

import java.util.Comparator;
import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollectionItem;
import org.folio.rest.jaxrs.model.ResourceBulkFetchDataAttributes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceBulkFetchCollectionItemConverter implements Converter<Title, ResourceBulkFetchCollectionItem> {

  private final Converter<List<CoverageDates>, List<Coverage>> coverageDatesConverter;

  public ResourceBulkFetchCollectionItemConverter(Converter<List<CoverageDates>, List<Coverage>> converter) {
    this.coverageDatesConverter = converter;
  }

  @Override
  public ResourceBulkFetchCollectionItem convert(Title title) {
    CustomerResources resource = title.getCustomerResourcesList().getFirst();
    return new ResourceBulkFetchCollectionItem()
      .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
      .withType(ResourceBulkFetchCollectionItem.Type.RESOURCES)
      .withAttributes(new ResourceBulkFetchDataAttributes()
        .withName(title.getTitleName())
        .withProviderId(resource.getVendorId())
        .withProviderName(resource.getVendorName())
        .withPackageId(resource.getVendorId() + "-" + resource.getPackageId())
        .withTitleId(title.getTitleId())
        .withPublicationType(ConverterConsts.PUBLICATION_TYPES.get(title.getPubType().toLowerCase()))
        .withManagedCoverages(coverageDatesConverter.convert(resource.getManagedCoverageList()))
        .withCustomCoverages(coverageDatesConverter.convert(
          resource.getCustomCoverageList().stream()
            .sorted(Comparator.comparing(CoverageDates::getBeginCoverage).reversed())
            .toList()))
        .withCoverageStatement(resource.getCoverageStatement())
      );
  }
}
