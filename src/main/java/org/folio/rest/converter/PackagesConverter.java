package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackageDataAttributes.ContentType;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.TokenInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

  public Package convert(PackageByIdData packageByIdData) {
    PackageCollectionItem packageCollectionItem = convertPackage(packageByIdData);
    packageCollectionItem
      .withId(packageByIdData.getVendorId() + "-" + packageByIdData.getPackageId())
      .withRelationships(EMPTY_PACKAGES_RELATIONSHIP)
      .withType(PACKAGES_TYPE)
      .getAttributes()
        .withProxy(convertToProxy(packageByIdData.getProxy()))
        .withPackageToken(convertToToken(packageByIdData.getPackageToken()));
    return new Package()
      .withData(packageCollectionItem)
      .withJsonapi(RestConstants.JSONAPI);
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

  private Token convertToToken(TokenInfo packageToken) {
    if(Objects.isNull(packageToken)){
      return null;
    }
    return new Token()
      .withFactName(packageToken.getFactName())
      .withHelpText(packageToken.getHelpText())
      .withPrompt(packageToken.getPrompt())
      .withValue(packageToken.getValue() == null ? null : (String) packageToken.getValue());
  }

  private Proxy convertToProxy(org.folio.rmapi.model.Proxy proxy) {
    return proxy != null ? new Proxy().withId(proxy.getId())
      .withInherited(proxy.getInherited()) : null;
  }
}
