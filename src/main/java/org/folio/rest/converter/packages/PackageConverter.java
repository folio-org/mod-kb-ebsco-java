package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.packages.PackageConverterUtils.createEmptyPackageRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.RESOURCES_TYPE;

import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.TokenInfo;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.HasManyRelationship;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.PackageResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PackageConverter implements Converter<PackageResult, Package> {

  private final Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;
  private final Converter<VendorById, Provider> vendorConverter;
  private final Converter<Titles, ResourceCollection> resourcesConverter;
  private final Converter<TokenInfo, Token> tokenInfoConverter;

  public PackageConverter(Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter,
                          Converter<VendorById, Provider> vendorConverter,
                          Converter<Titles, ResourceCollection> resourcesConverter,
                          Converter<TokenInfo, Token> tokenInfoConverter) {
    this.packageCollectionItemConverter = packageCollectionItemConverter;
    this.vendorConverter = vendorConverter;
    this.resourcesConverter = resourcesConverter;
    this.tokenInfoConverter = tokenInfoConverter;
  }

  @Override
  public Package convert(@NonNull PackageResult result) {
    PackageByIdData packageByIdData = result.getPackageData();
    Titles titles = result.getTitles();
    VendorById vendor = result.getVendor();

    Package packageData = new Package()
      .withData(packageCollectionItemConverter.convert(packageByIdData))
      .withJsonapi(RestConstants.JSONAPI);

    packageData.getData()
      .withRelationships(createEmptyPackageRelationship())
      .withType(PACKAGES_TYPE)
      .getAttributes()
      .withProxy(convertToProxy(packageByIdData.getProxy()))
      .withPackageToken(tokenInfoConverter.convert(packageByIdData.getPackageToken()))
      .withTags(result.getTags());

    if (titles != null) {
      packageData.getData()
        .withRelationships(new PackageRelationship()
          .withResources(new HasManyRelationship()
            .withMeta(new MetaDataIncluded()
              .withIncluded(true))
            .withData(convertResourcesRelationship(packageByIdData, titles))));

      packageData
        .getIncluded()
        .addAll(Objects.requireNonNull(resourcesConverter.convert(titles)).getData());
    }

    if (vendor != null) {
      packageData.getIncluded().add(Objects.requireNonNull(vendorConverter.convert(vendor)).getData());
      packageData.getData()
        .getRelationships()
        .withProvider(new HasOneRelationship()
          .withData(new RelationshipData()
            .withId(String.valueOf(vendor.getVendorId()))
            .withType(PROVIDERS_TYPE)));
    }

    AccessType accessType = result.getAccessType();
    if (accessType != null) {
      packageData.getIncluded().add(accessType);
      packageData.getData()
        .getRelationships()
        .withAccessType(new HasOneRelationship()
          .withData(new RelationshipData()
            .withId(accessType.getId())
            .withType(AccessType.Type.ACCESS_TYPES.value()))
          .withMeta(new MetaDataIncluded()
            .withIncluded(true)));
    }
    return packageData;
  }

  private Proxy convertToProxy(org.folio.holdingsiq.model.Proxy proxy) {
    return proxy != null ? new Proxy().withId(proxy.getId()).withInherited(proxy.getInherited()) : null;
  }

  private List<RelationshipData> convertResourcesRelationship(PackageByIdData packageByIdData, Titles titles) {
    return mapItems(titles.getTitleList(),
      title -> new RelationshipData()
        .withId(packageByIdData.getVendorId() + "-" + packageByIdData.getPackageId() + "-" + title.getTitleId())
        .withType(RESOURCES_TYPE));
  }
}
