package org.folio.rest.converter;

import com.google.common.collect.ImmutableMap;
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
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.TokenInfo;

public class PackagesConverter {

  private static final Map<String, String> FILTER_TYPE_MAPPING =
    ImmutableMap.<String, String>builder()
      .put("aggregatedfulltext", "Aggregated Full Text")
      .put("abstractandindex", "Abstract and Index")
      .put("ebook", "E-Book")
      .put("ejournal", "E-Journal")
      .put("print", "Print")
      .put("unknown", "Unknown")
      .put("onlinereference", "Online Reference").build();
  private static final PackageRelationship EMPTY_PACKAGES_RELATIONSHIP = new PackageRelationship()
    .withProvider(new MetaIncluded()
      .withMeta(new MetaDataIncluded().withIncluded(false)))
    .withResources(new MetaIncluded()
      .withMeta(new MetaDataIncluded()
        .withIncluded(false)));
  private static final String PACKAGES_TYPE = "packages";

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
    TokenInfo packageToken = packageData.getPackageToken();
    org.folio.rmapi.model.Proxy proxy = packageData.getProxy();
    return new PackageCollectionItem()
      .withId(providerId + "-" + packageId)
      .withType(PACKAGES_TYPE)
      .withAttributes(new PackageDataAttributes()
        .withContentType(FILTER_TYPE_MAPPING.get(packageData.getContentType().toLowerCase()))
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
                : ""))
        .withProxy(proxy == null ? null : convertToProxy(proxy))
        .withPackageToken(packageToken == null ? null : convertToToken(packageToken)))
      .withRelationships(EMPTY_PACKAGES_RELATIONSHIP);
  }

  private Token convertToToken(TokenInfo packageToken) {
    return new Token()
      .withFactName(packageToken.getFactName())
      .withHelpText(packageToken.getHelpText())
      .withPrompt(packageToken.getPrompt())
      .withValue(packageToken.getValue() == null ? null : (String) packageToken.getValue());
  }

  private Proxy convertToProxy(org.folio.rmapi.model.Proxy proxy) {
    return new Proxy().withId(proxy.getId())
      .withInherited(proxy.getInherited());
  }
}
