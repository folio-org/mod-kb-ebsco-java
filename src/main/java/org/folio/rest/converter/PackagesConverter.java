package org.folio.rest.converter;

import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.RESOURCES_TYPE;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.HasManyRelationship;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.PackagePost;
import org.folio.rmapi.model.PackagePut;
import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.Titles;
import org.folio.rmapi.model.TokenInfo;
import org.folio.rmapi.model.VendorById;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PackagesConverter {

  private static final Map<String, ContentType> contentTypes = new HashMap<>();

  private static final Map<ContentType, Integer> contentTypeToRMAPICode = new EnumMap<>(ContentType.class);

  static {
    contentTypes.put("aggregatedfulltext", ContentType.AGGREGATED_FULL_TEXT);
    contentTypes.put("abstractandindex", ContentType.ABSTRACT_AND_INDEX);
    contentTypes.put("ebook", ContentType.E_BOOK);
    contentTypes.put("ejournal", ContentType.E_JOURNAL);
    contentTypes.put("print", ContentType.PRINT);
    contentTypes.put("unknown", ContentType.UNKNOWN);
    contentTypes.put("onlinereference", ContentType.ONLINE_REFERENCE);
  }

  static {
    contentTypeToRMAPICode.put(ContentType.AGGREGATED_FULL_TEXT, 1);
    contentTypeToRMAPICode.put(ContentType.ABSTRACT_AND_INDEX, 2);
    contentTypeToRMAPICode.put(ContentType.E_BOOK, 3);
    contentTypeToRMAPICode.put(ContentType.E_JOURNAL, 4);
    contentTypeToRMAPICode.put(ContentType.PRINT, 5);
    contentTypeToRMAPICode.put(ContentType.UNKNOWN, 6);
    contentTypeToRMAPICode.put(ContentType.ONLINE_REFERENCE, 7);
  }

  @Autowired
  private CommonAttributesConverter commonConverter;
  @Autowired
  private VendorConverter vendorConverter;
  @Autowired
  private ResourcesConverter resourcesConverter;

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
    return convert(packageByIdData, null, null);
  }

  public Package convert(PackageByIdData packageByIdData, VendorById vendor, Titles titles) {
    Package packageData = new Package()
      .withData(convertPackage(packageByIdData))
      .withJsonapi(RestConstants.JSONAPI);

    packageData.getData()
      .withRelationships(createEmptyPackageRelationship())
      .withType(PACKAGES_TYPE)
      .getAttributes()
        .withProxy(convertToProxy(packageByIdData.getProxy()))
        .withPackageToken(commonConverter.convertToken(packageByIdData.getPackageToken()));

    if (titles != null) {
      packageData.getData()
        .withRelationships(new PackageRelationship()
          .withResources(new HasManyRelationship()
            .withMeta(new MetaDataIncluded()
              .withIncluded(true))
            .withData(convertResourcesRelationship(packageByIdData, titles))));

      packageData
        .getIncluded()
          .addAll(resourcesConverter.convertFromRMAPIResourceList(titles).getData());
    }

    if (vendor != null) {
      packageData.getIncluded().add(vendorConverter.convertToProvider(vendor).getData());
      packageData.getData()
        .getRelationships()
        .withProvider(new HasOneRelationship()
          .withData(new RelationshipData()
            .withId(String.valueOf(vendor.getVendorId()))
            .withType(PROVIDERS_TYPE)));
    }

    return packageData;
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
      .withRelationships(createEmptyPackageRelationship());
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
  public PackagePost convertToPackage(PackagePostRequest postPackageBody) {

    PackagePost.PackagePostBuilder postRequest = PackagePost.builder()
      .contentType(contentTypeToRMAPICode.getOrDefault(postPackageBody.getData().getAttributes().getContentType(), 6))
      .packageName(postPackageBody.getData().getAttributes().getName());

    Coverage customCoverage = postPackageBody.getData().getAttributes().getCustomCoverage();
    if (customCoverage != null) {
      postRequest.coverage(
        org.folio.rmapi.model.CoverageDates.builder()
          .beginCoverage(customCoverage.getBeginCoverage())
          .endCoverage(customCoverage.getEndCoverage())
          .build());
    }

    return postRequest.build();
  }

  private List<RelationshipData> convertResourcesRelationship(PackageByIdData packageByIdData, Titles titles) {
    return titles.getTitleList().stream()
      .map(title ->
        new RelationshipData()
          .withId(packageByIdData.getVendorId() + "-" + packageByIdData.getPackageId() + "-" + title.getTitleId())
          .withType(RESOURCES_TYPE))
      .collect(Collectors.toList());
  }

  private static PackageRelationship createEmptyPackageRelationship() {
    return new PackageRelationship()
      .withProvider(new HasOneRelationship()
        .withMeta(new MetaDataIncluded().withIncluded(false)))
      .withResources(new HasManyRelationship()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false)));
  }
}
