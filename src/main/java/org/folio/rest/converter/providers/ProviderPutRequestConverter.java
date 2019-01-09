package org.folio.rest.converter.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rmapi.model.VendorPut;
import org.folio.rmapi.model.VendorPutToken;

@Component
public class ProviderPutRequestConverter implements Converter<ProviderPutRequest, VendorPut> {

  @Autowired
  private Converter<org.folio.rmapi.model.Packages, PackageCollection> packagesConverter;

  @Override
  public VendorPut convert(@NonNull ProviderPutRequest provider) {
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
