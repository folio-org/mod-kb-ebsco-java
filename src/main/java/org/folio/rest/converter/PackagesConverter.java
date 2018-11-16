package org.folio.rest.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackageDataAttributes.ContentType;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.Packages;

public class PackagesConverter {

  private static final Map<String, PackageDataAttributes.ContentType> contentType = new HashMap<>();
  private static final PackageRelationship EMPTY_PACKAGES_RELATIONSHIP = new PackageRelationship()
    .withProvider(new MetaIncluded()
      .withMeta(new MetaDataIncluded().withIncluded(false)))
    .withResources(new MetaIncluded()
      .withMeta(new MetaDataIncluded()
        .withIncluded(false)));
  private static final String PACKAGES_TYPE = "packages";

  static {
    contentType.put("aggregatedfulltext", ContentType.AGGREGATED_FULL_TEXT);
    contentType.put("abstractandindex", ContentType.ABSTRACT_AND_INDEX);
    contentType.put("ebook", ContentType.E_BOOK);
    contentType.put("ejournal", ContentType.E_JOURNAL);
    contentType.put("print", ContentType.PRINT);
    contentType.put("unknown", ContentType.UNKNOWN);
    contentType.put("onlinereference", ContentType.ONLINE_REFERENCE);
  }

  public PackageCollection convert(Packages packages) {
    List<PackageCollectionItem> packageList = packages.getPackagesList().stream()
      .map(this::convertPackage)
      .collect(Collectors.toList());
    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }

  private PackageCollectionItem convertPackage(PackageData packageData) {
    Integer providerId = packageData.getVendorId();
    String providerName = packageData.getVendorName();
    Integer packageId = packageData.getPackageId();
    return new PackageCollectionItem()
      .withId(providerId + "-" + packageId)
      .withType(PACKAGES_TYPE)
      .withAttributes(new PackageDataAttributes()
        .withContentType(contentType.get(packageData.getContentType().toLowerCase()))
        .withCustomCoverage(
          new Coverage()
            .withBeginCoverage(packageData.getCustomCoverage().getBeginCoverage())
            .withEndCoverage(packageData.getCustomCoverage().getEndCoverage()))
        .withIsCustom(packageData.getCustom())
        .withIsSelected(packageData.getSelected())
        .withName(packageData.getPackageName())
        .withPackageId(packageId)
        .withPackageType(packageData.getPackageType())
        .withProviderId(providerId)
        .withProviderName(providerName)
        .withSelectedCount(packageData.getSelectedCount())
        .withTitleCount(packageData.getTitleCount())
        .withAllowKbToAddTitles(packageData.getAllowEbscoToAddTitles())
        .withVisibilityData(
          new VisibilityData().withIsHidden(packageData.getVisibilityData().getHidden())
            .withReason(
              packageData.getVisibilityData().getReason().equals("Hidden by EP") ? "Set by system"
                : "")))
      .withRelationships(EMPTY_PACKAGES_RELATIONSHIP);
  }

}
