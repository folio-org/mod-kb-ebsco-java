package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.packages.PackageConverterUtils.createEmptyPackageRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.RESOURCES_TYPE;

import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Proxy;
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
import org.folio.rest.jaxrs.model.ProxyDto;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.PackageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
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
  public Package convert(PackageResult result) {
    PackageData packageData = result.getPackageData();

    Package packageResult = new Package()
      .withData(packageCollectionItemConverter.convert(packageData))
      .withJsonapi(RestConstants.JSONAPI);

    packageResult.getData()
      .withRelationships(createEmptyPackageRelationship())
      .withType(PACKAGES_TYPE)
      .getAttributes()
      .withProxy(convertToProxy(packageData.getProxy()))
      .withPackageToken(tokenInfoConverter.convert(packageData.getPackageToken()))
      .withTags(result.getTags());

    addTitlesRelationship(result, packageResult, packageData);
    addProviderRelationship(result, packageResult);
    addAccessTypeRelationship(result, packageResult);
    return packageResult;
  }

  private void addTitlesRelationship(PackageResult result, Package packageDto, PackageData packageData) {
    Titles titles = result.getTitles();
    if (titles != null) {
      packageDto.getData()
        .withRelationships(new PackageRelationship()
          .withResources(new HasManyRelationship()
            .withMeta(new MetaDataIncluded()
              .withIncluded(true))
            .withData(convertResourcesRelationship(packageData, titles))));

      packageDto
        .getIncluded()
        .addAll(Objects.requireNonNull(resourcesConverter.convert(titles)).getData());
    }
  }

  private void addProviderRelationship(PackageResult result, Package packageData) {
    VendorById vendor = result.getVendor();
    if (vendor != null) {
      packageData.getIncluded().add(Objects.requireNonNull(vendorConverter.convert(vendor)).getData());
      packageData.getData()
        .getRelationships()
        .withProvider(new HasOneRelationship()
          .withData(new RelationshipData()
            .withId(String.valueOf(vendor.getVendorId()))
            .withType(PROVIDERS_TYPE)));
    }
  }

  private void addAccessTypeRelationship(PackageResult result, Package packageData) {
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
  }

  private @Nullable ProxyDto convertToProxy(@Nullable Proxy proxy) {
    return proxy != null ? new ProxyDto().withId(proxy.getId()).withInherited(proxy.getInherited()) : null;
  }

  private List<RelationshipData> convertResourcesRelationship(PackageData packageData, Titles titles) {
    return mapItems(titles.getTitleList(),
      title -> new RelationshipData()
        .withId(packageData.getVendorId() + "-" + packageData.getPackageId() + "-" + title.getTitleId())
        .withType(RESOURCES_TYPE));
  }
}
