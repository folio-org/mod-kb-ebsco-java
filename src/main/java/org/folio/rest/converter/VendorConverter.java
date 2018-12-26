package org.folio.rest.converter;

import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.ProviderListDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.TokenInfo;
import org.folio.rmapi.model.Vendor;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.VendorPut;
import org.folio.rmapi.model.VendorPutToken;
import org.folio.rmapi.model.Vendors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class VendorConverter {

  @Autowired
  private CommonAttributesConverter commonConverter;

  @Autowired
  private Converter<org.folio.rmapi.model.Packages, PackageCollection> packagesConverter;

  public ProviderCollection convert(Vendors vendors) {
    List<Providers> providerList = vendors.getVendorList().stream()
      .map(this::convertVendor)
      .collect(Collectors.toList());
    return new ProviderCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(vendors.getTotalResults()))
      .withData(providerList);
  }

  private Providers convertVendor(Vendor vendor) {
    String token = vendor.getVendorToken();
    return new Providers()
      .withId(String.valueOf(vendor.getVendorId()))
      .withType(PROVIDERS_TYPE)
      .withAttributes(new ProviderListDataAttributes()
        .withName(vendor.getVendorName())
        .withPackagesTotal(vendor.getPackagesTotal())
        .withPackagesSelected(vendor.getPackagesSelected())
        .withSupportsCustomPackages(vendor.isCustomer())
        .withProviderToken(token != null ? new Token().withValue(token) : null))
      .withRelationships(createEmptyProviderRelationships());
  }

  public Provider convertToProvider(VendorById vendor) {
    return convertToProvider(vendor, null);
  }

  public Provider convertToProvider(VendorById vendor, org.folio.rmapi.model.Packages packages) {
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

  public VendorPut convertToVendor(ProviderPutRequest provider) {

    VendorPut.VendorPutBuilder vpb = VendorPut.builder();

    // RM API gives an error when we pass inherited as true along with updated proxy
    // value
    // Hard code it to false; it should not affect the state of inherited that RM
    // API maintains
    if (provider.getData().getAttributes().getProxy() != null) {
      vpb.proxy(org.folio.rmapi.model.Proxy.builder()
        .inherited(false)
        .id(provider.getData().getAttributes().getProxy().getId())
        .build());
    }

    if (provider.getData().getAttributes().getProviderToken() != null) {
      vpb.vendorToken(VendorPutToken.builder()
        .value(provider.getData().getAttributes().getProviderToken().getValue())
        .build());
    }

    return vpb.build();

  }

  private static Relationships createEmptyProviderRelationships() {
    return new Relationships()
      .withPackages(new Packages()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false))
        .withData(null));
  }
}
