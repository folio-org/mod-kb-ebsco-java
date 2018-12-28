package org.folio.rest.converter.providers;

import static org.folio.rest.converter.providers.ProviderRequestConverter.createEmptyProviderRelationships;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.converter.util.CommonAttributesConverter;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.TokenInfo;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.result.VendorResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProviderConverter implements Converter<VendorResult, Provider> {

  @Autowired
  CommonAttributesConverter commonConverter;
  @Autowired
  private Converter<org.folio.rmapi.model.Packages, PackageCollection> packagesConverter;

  @Override
  public Provider convert(VendorResult result) {
    VendorById vendor = result.getVendor();
    org.folio.rmapi.model.Packages packages = result.getPackages();

    TokenInfo vendorToken = vendor.getVendorByIdToken();
    Provider provider = new Provider()
      .withData(new ProviderData()
        .withId(String.valueOf(vendor.getVendorId()))
        .withType(PROVIDERS_TYPE)
        .withAttributes(new ProviderDataAttributes()
          .withName(vendor.getVendorName())
          .withPackagesTotal(vendor.getPackagesTotal())
          .withPackagesSelected(vendor.getPackagesSelected())
          .withSupportsCustomPackages(vendor.isCustomer())
          .withProviderToken(commonConverter.convertToken(vendorToken))
          .withProxy(new Proxy()
            .withId(vendor.getProxy().getId())
            .withInherited(vendor.getProxy().getInherited()))
        )
        .withRelationships(createEmptyProviderRelationships()))
      .withJsonapi(RestConstants.JSONAPI);
    if(packages != null){
      provider
        .withIncluded(packagesConverter.convert(packages).getData())
        .getData()
        .withRelationships(new Relationships()
          .withPackages(new Packages()
            .withMeta(new MetaDataIncluded().withIncluded(true))
            .withData(convertPackagesRelationship(packages))));

    }
    return provider;
  }

  private List<RelationshipData> convertPackagesRelationship(org.folio.rmapi.model.Packages packages) {
    return packages.getPackagesList().stream()
      .map(packageData ->
        new RelationshipData()
          .withId(packageData.getVendorId() + "-" + packageData.getPackageId())
          .withType(PACKAGES_TYPE))
      .collect(Collectors.toList());
  }
}
