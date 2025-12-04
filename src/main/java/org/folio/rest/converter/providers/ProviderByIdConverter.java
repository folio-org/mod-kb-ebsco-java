package org.folio.rest.converter.providers;

import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rmapi.result.VendorResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProviderByIdConverter implements Converter<VendorById, Provider> {

  private final Converter<VendorResult, Provider> vendorConverter;

  public ProviderByIdConverter(Converter<VendorResult, Provider> vendorConverter) {
    this.vendorConverter = vendorConverter;
  }

  @Override
  public Provider convert(VendorById vendor) {
    return vendorConverter.convert(new VendorResult(vendor, null));
  }
}
