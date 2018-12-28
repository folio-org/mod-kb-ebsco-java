package org.folio.rest.converter.providers;

import org.folio.rest.converter.util.CommonAttributesConverter;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rmapi.model.VendorPut;
import org.folio.rmapi.model.VendorPutToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProviderRequestConverter {

  @Autowired
  private CommonAttributesConverter commonConverter;

  @Autowired
  private Converter<org.folio.rmapi.model.Packages, PackageCollection> packagesConverter;

  public static Relationships createEmptyProviderRelationships() {
    return new Relationships()
      .withPackages(new Packages()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false))
        .withData(null));
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
}
