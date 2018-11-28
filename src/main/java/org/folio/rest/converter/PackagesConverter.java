package org.folio.rest.converter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.PackagePut;
import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.TokenInfo;

public class PackagesConverter {

  private static final Map<String, PackageDataAttributes.ContentType> contentTypes = new HashMap<>();
  private static final PackageRelationship EMPTY_PACKAGES_RELATIONSHIP = new PackageRelationship()
    .withProvider(new MetaIncluded()
      .withMeta(new MetaDataIncluded().withIncluded(false)))
    .withResources(new MetaIncluded()
      .withMeta(new MetaDataIncluded()
        .withIncluded(false)));
  private static final String PACKAGES_TYPE = "packages";
  private static final Map<PackageDataAttributes.ContentType, Integer> contentTypeToRMAPICode = new EnumMap<>(PackageDataAttributes.ContentType.class);

  static {
    contentTypes.put("aggregatedfulltext", PackageDataAttributes.ContentType.AGGREGATED_FULL_TEXT);
    contentTypes.put("abstractandindex", PackageDataAttributes.ContentType.ABSTRACT_AND_INDEX);
    contentTypes.put("ebook", PackageDataAttributes.ContentType.E_BOOK);
    contentTypes.put("ejournal", PackageDataAttributes.ContentType.E_JOURNAL);
    contentTypes.put("print", PackageDataAttributes.ContentType.PRINT);
    contentTypes.put("unknown", PackageDataAttributes.ContentType.UNKNOWN);
    contentTypes.put("onlinereference", PackageDataAttributes.ContentType.ONLINE_REFERENCE);
  }

  static {
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.AGGREGATED_FULL_TEXT, 1);
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.ABSTRACT_AND_INDEX, 2);
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.E_BOOK, 3);
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.E_JOURNAL, 4);
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.PRINT, 5);
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.UNKNOWN, 6);
    contentTypeToRMAPICode.put(PackageDataAttributes.ContentType.ONLINE_REFERENCE, 7);
  }

  private CommonAttributesConverter commonConverter;

  public PackagesConverter() {
    this(new CommonAttributesConverter());
  }

  public PackagesConverter(CommonAttributesConverter commonConverter) {
    this.commonConverter = commonConverter;
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
      .withPackageToken(commonConverter.convertToken(packageByIdData.getPackageToken()));
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
      .withRelationships(EMPTY_PACKAGES_RELATIONSHIP);
  }

  private Proxy convertToProxy(org.folio.rmapi.model.Proxy proxy) {
    return proxy != null ? new Proxy().withId(proxy.getId())
      .withInherited(proxy.getInherited()) : null;
  }

  public PackagePut convertToRMAPICustomPackagePutRequest(PackagePutRequest request) {
    PackageDataAttributes attributes = request.getData().getAttributes();
    PackagePut.PackagePutBuilder builder = convertCommonAttributesToPackagePutRequest(attributes);
    builder.packageName(attributes.getName());
    Integer contentType = contentTypeToRMAPICode.get(attributes.getContentType());
    builder.contentType(contentType != null ? contentType : 6);
    return builder.build();
  }

  public PackagePut convertToRMAPIPackagePutRequest(PackagePutRequest request) {
    PackageDataAttributes attributes = request.getData().getAttributes();
    PackagePut.PackagePutBuilder builder = convertCommonAttributesToPackagePutRequest(attributes);
    builder.allowEbscoToAddTitles(attributes.getAllowKbToAddTitles());
    if (attributes.getPackageToken() != null) {
      TokenInfo tokenInfo = TokenInfo.builder()
        .value(attributes.getPackageToken().getValue())
        .build();
      builder.packageToken(tokenInfo);
    }
    return builder.build();
  }

  private PackagePut.PackagePutBuilder convertCommonAttributesToPackagePutRequest(PackageDataAttributes attributes) {
    PackagePut.PackagePutBuilder builder = PackagePut.builder();

    builder.isSelected(attributes.getIsSelected());

    if (attributes.getProxy() != null) {
      org.folio.rmapi.model.Proxy proxy = org.folio.rmapi.model.Proxy.builder()
        .id(attributes.getProxy().getId())
//    RM API gives an error when we pass inherited as true along with updated proxy value
//    Hard code it to false; it should not affect the state of inherited that RM API maintains
        .inherited(false)
        .build();
      builder.proxy(proxy);
    }

    if (attributes.getVisibilityData() != null) {
      builder.isHidden(attributes.getVisibilityData().getIsHidden());
    }

    if (attributes.getCustomCoverage() != null) {
      CoverageDates coverageDates = CoverageDates.builder()
        .beginCoverage(attributes.getCustomCoverage().getBeginCoverage())
        .endCoverage(attributes.getCustomCoverage().getEndCoverage())
        .build();
      builder.customCoverage(coverageDates);
    }

    return builder;
  }
}
