package org.folio.rest.converter.resources;

import java.util.Comparator;
import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.Resource;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollectionItem;
import org.folio.rest.jaxrs.model.ResourceBulkFetchDataAttributes;
import org.folio.rest.util.IdParser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceBulkFetchCollectionItemConverter
  implements Converter<Resource, ResourceBulkFetchCollectionItem> {

  private final Converter<List<CoverageDates>, List<Coverage>> coverageDatesConverter;

  public ResourceBulkFetchCollectionItemConverter(Converter<List<CoverageDates>, List<Coverage>> converter) {
    this.coverageDatesConverter = converter;
  }

  @Override
  public ResourceBulkFetchCollectionItem convert(Resource resource) {
    return new ResourceBulkFetchCollectionItem()
      .withId(IdParser.getResourceId(resource))
      .withType(ResourceBulkFetchCollectionItem.Type.RESOURCES)
      .withAttributes(new ResourceBulkFetchDataAttributes()
        .withName(resource.getTitleName())
        .withProviderId(resource.getVendorId())
        .withProviderName(resource.getVendorName())
        .withPackageId(resource.getVendorId() + "-" + resource.getPackageId())
        .withTitleId(resource.getTitleId())
        .withPublicationType(ConverterConsts.PUBLICATION_TYPES.get(resource.getPubType().toLowerCase()))
        .withManagedCoverages(coverageDatesConverter.convert(resource.getManagedCoverageList()))
        .withCustomCoverages(coverageDatesConverter.convert(
          resource.getCustomCoverageList().stream()
            .sorted(Comparator.comparing(CoverageDates::getBeginCoverage).reversed())
            .toList()))
        .withCoverageStatement(resource.getCoverageStatement())
      );
  }
}
