package org.folio.rest.converter.providers;

import static org.folio.rest.converter.providers.ProviderRequestConverter.createEmptyProviderRelationships;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;

import org.folio.rest.jaxrs.model.ProviderListDataAttributes;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rmapi.model.Vendor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProvidersConverter implements Converter<Vendor, Providers> {
  @Override
  public Providers convert(Vendor vendor) {
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
}
