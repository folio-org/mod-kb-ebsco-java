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
import org.folio.rmapi.result.VendorResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class VendorConverter {

  @Autowired
  private CommonAttributesConverter commonConverter;

  @Autowired
  private Converter<org.folio.rmapi.model.Packages, PackageCollection> packagesConverter;

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

  public static Relationships createEmptyProviderRelationships() {
    return new Relationships()
      .withPackages(new Packages()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false))
        .withData(null));
  }
}
