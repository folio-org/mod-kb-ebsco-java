package org.folio.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.ProviderListDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Vendor;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.VendorToken;
import org.folio.rmapi.model.Vendors;

public class VendorConverter {

  private static final Relationships EMPTY_PACKAGE_RELATIONSHIP = new Relationships()
      .withPackages(new Packages()
          .withMeta(new MetaDataIncluded()
              .withIncluded(false))
          .withData(null));
  private final String PROVIDERS_TYPE = "providers";

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
    VendorToken token = vendor.getVendorToken();
    return new Providers()
        .withId(String.valueOf(vendor.getVendorId()))
        .withType(PROVIDERS_TYPE)
        .withAttributes(new ProviderListDataAttributes()
            .withName(vendor.getVendorName())
            .withPackagesTotal(vendor.getPackagesTotal()).withPackagesSelected(vendor.getPackagesSelected())
            .withSupportsCustomPackages(vendor.isCustomer())
            .withProviderToken(token != null ? new Token().withValue((String) token.getValue()) : null))
        .withRelationships(EMPTY_PACKAGE_RELATIONSHIP);
  }

  public Provider convertToProvider(VendorById vendor) {
    VendorToken vendorToken = vendor.getVendorToken();
    return new Provider()
        .withData(new ProviderData()
            .withId(String.valueOf(vendor.getVendorId()))
            .withType(PROVIDERS_TYPE)
            .withAttributes(new ProviderDataAttributes()
                .withName(vendor.getVendorName())
                .withPackagesTotal(vendor.getPackagesTotal())
                .withPackagesSelected(vendor.getPackagesSelected())
                .withSupportsCustomPackages(vendor.isCustomer())
                .withProviderToken(vendorToken != null ? convertToken(vendorToken) : null)
                .withProxy(new Proxy()
                    .withId(vendor.getProxy().getId())
                    .withInherited(vendor.getProxy().getInherited())))
            .withRelationships(EMPTY_PACKAGE_RELATIONSHIP))
        .withIncluded(null)
        .withJsonapi(RestConstants.JSONAPI);
  }

  public VendorById convertToVendor(ProviderPutRequest provider) {

    VendorById vendor = new VendorById();

    // RM API gives an error when we pass inherited as true along with updated proxy
    // value
    // Hard code it to false; it should not affect the state of inherited that RM
    // API maintains
    org.folio.rmapi.model.Proxy vendorProxy = new org.folio.rmapi.model.Proxy();
    org.folio.rmapi.model.VendorToken vendorToken = new org.folio.rmapi.model.VendorToken();

    if (provider.getData().getAttributes().getProxy() != null) {
      vendorProxy.setInherited(false);
      vendorProxy.setId(provider.getData().getAttributes().getProxy().getId());
      vendor.setProxy(vendorProxy);
    }

    if (provider.getData().getAttributes().getProviderToken() != null) {
      vendorToken.setValue(provider.getData().getAttributes().getProviderToken().getValue());
      vendor.setVendorToken(vendorToken);
    }

    return vendor;

  }

  private Token convertToken(VendorToken vendorToken) {
    return new Token()
        .withFactName(vendorToken.getFactName())
        .withHelpText(vendorToken.getHelpText())
        .withPrompt(vendorToken.getPrompt())
        .withValue(vendorToken.getValue() == null ? null : (String) vendorToken.getValue());
  }
}
