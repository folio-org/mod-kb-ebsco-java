package org.folio.rest.converter.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rmapi.result.VendorResult;

@Component
public class ProviderByIdConverter implements Converter<VendorById, Provider> {

  @Autowired
  private Converter<VendorResult, Provider> vendorConverter;

  @Override
  public Provider convert(VendorById vendor) {
    return vendorConverter.convert(new VendorResult(vendor, null));
  }
}
