package org.folio.rest.converter.packages;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.HasManyRelationship;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.PackagePost;
import org.folio.rmapi.model.PackagePut;
import org.folio.rmapi.model.TokenInfo;
import org.springframework.stereotype.Component;

@Component
public class PackageRequestConverter {

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

  public static PackageRelationship createEmptyPackageRelationship() {
    return new PackageRelationship()
      .withProvider(new HasOneRelationship()
        .withMeta(new MetaDataIncluded().withIncluded(false)))
      .withResources(new HasManyRelationship()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false)));
  }
}
