package org.folio.rest.converter.providers;

import static org.folio.rest.converter.providers.ProviderConverterUtils.createEmptyProviderRelationships;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Vendor;
import org.folio.rest.jaxrs.model.ProviderListDataAttributes;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Token;

@Component
public class ProvidersConverter implements Converter<Vendor, Providers> {

  @Override
  public Providers convert(@NonNull Vendor vendor) {
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
