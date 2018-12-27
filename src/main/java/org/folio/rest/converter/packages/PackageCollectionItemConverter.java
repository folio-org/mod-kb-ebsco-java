package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.PackageRequestConverter.createEmptyPackageRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rmapi.model.PackageData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageCollectionItemConverter implements Converter<PackageData, PackageCollectionItem> {
  private static final Map<String, ContentType> contentTypes = new HashMap<>();

  static {
    contentTypes.put("aggregatedfulltext", ContentType.AGGREGATED_FULL_TEXT);
    contentTypes.put("abstractandindex", ContentType.ABSTRACT_AND_INDEX);
    contentTypes.put("ebook", ContentType.E_BOOK);
    contentTypes.put("ejournal", ContentType.E_JOURNAL);
    contentTypes.put("print", ContentType.PRINT);
    contentTypes.put("unknown", ContentType.UNKNOWN);
    contentTypes.put("onlinereference", ContentType.ONLINE_REFERENCE);
  }

  @Override
  public PackageCollectionItem convert(PackageData packageData) {
    Integer providerId = packageData.getVendorId();
    String providerName = packageData.getVendorName();
    Integer packageId = packageData.getPackageId();
    return new PackageCollectionItem()
      .withId(providerId + "-" + packageId)
      .withType(PACKAGES_TYPE)
      .withAttributes(new PackageDataAttributes()
        .withContentType(contentTypes.get(packageData.getContentType().toLowerCase()))
        .withCustomCoverage(
          new Coverage()
            .withBeginCoverage(packageData.getCustomCoverage().getBeginCoverage())
            .withEndCoverage(packageData.getCustomCoverage().getEndCoverage()))
        .withIsCustom(packageData.getIsCustom())
        .withIsSelected(packageData.getIsSelected())
        .withName(packageData.getPackageName())
        .withPackageId(packageId)
        .withPackageType(packageData.getPackageType())
        .withProviderId(providerId)
        .withProviderName(providerName)
        .withSelectedCount(packageData.getSelectedCount())
        .withTitleCount(packageData.getTitleCount())
        .withAllowKbToAddTitles(packageData.getAllowEbscoToAddTitles())
        .withVisibilityData(
          new VisibilityData().withIsHidden(packageData.getVisibilityData().getIsHidden())
            .withReason(
              packageData.getVisibilityData().getReason().equals("Hidden by EP") ? "Set by system"
                : "")))
      .withRelationships(createEmptyPackageRelationship());
  }
}
