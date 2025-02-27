package org.folio.rest.converter.providers;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.providers.ProviderConverterUtils.createEmptyProviderRelationships;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;

import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.TokenInfo;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderGetDataAttributes;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.VendorResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ProviderConverter implements Converter<VendorResult, Provider> {

  private final Converter<org.folio.holdingsiq.model.Packages, PackageCollection> packagesConverter;
  private final Converter<TokenInfo, Token> tokenInfoConverter;

  public ProviderConverter(Converter<org.folio.holdingsiq.model.Packages, PackageCollection> packagesConverter,
                           Converter<TokenInfo, Token> tokenInfoConverter) {
    this.packagesConverter = packagesConverter;
    this.tokenInfoConverter = tokenInfoConverter;
  }

  @Override
  public Provider convert(@NonNull VendorResult result) {
    VendorById vendor = result.getVendor();
    org.folio.holdingsiq.model.Packages packages = result.getPackages();

    TokenInfo vendorToken = vendor.getVendorByIdToken();
    Provider provider = new Provider()
      .withData(new ProviderData()
        .withId(String.valueOf(vendor.getVendorId()))
        .withType(PROVIDERS_TYPE)
        .withAttributes(new ProviderGetDataAttributes()
          .withName(vendor.getVendorName())
          .withPackagesTotal(vendor.getPackagesTotal())
          .withPackagesSelected(vendor.getPackagesSelected())
          .withSupportsCustomPackages(vendor.isCustomer())
          .withProviderToken(tokenInfoConverter.convert(vendorToken))
          .withProxy(new Proxy()
            .withId(vendor.getProxy().getId())
            .withInherited(vendor.getProxy().getInherited()))
          .withTags(result.getTags())
        )
        .withRelationships(createEmptyProviderRelationships()))
      .withJsonapi(RestConstants.JSONAPI);
    if (packages != null) {
      provider
        .withIncluded(Objects.requireNonNull(packagesConverter.convert(packages)).getData())
        .getData()
        .withRelationships(new Relationships()
          .withPackages(new Packages()
            .withMeta(new MetaDataIncluded().withIncluded(true))
            .withData(convertPackagesRelationship(packages))));
    }
    return provider;
  }

  private List<RelationshipData> convertPackagesRelationship(org.folio.holdingsiq.model.Packages packages) {
    return mapItems(packages.getPackagesList(),
      packageData -> new RelationshipData()
        .withId(packageData.getVendorId() + "-" + packageData.getPackageId())
        .withType(PACKAGES_TYPE));
  }
}
