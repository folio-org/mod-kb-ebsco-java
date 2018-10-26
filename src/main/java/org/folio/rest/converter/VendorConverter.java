package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.Meta;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderListDataAttributes;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Vendor;
import org.folio.rmapi.model.Vendors;

import java.util.List;
import java.util.stream.Collectors;

public class VendorConverter {

  private static final Relationships EMPTY_PACKAGE_RELATIONSHIP = new Relationships()
    .withPackages(
      new MetaIncluded()
        .withMeta(
          new Meta()
            .withIncluded(false)));

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
      .withType("providers")
      .withAttributes(new ProviderListDataAttributes()
        .withName(vendor.getVendorName())
        .withPackagesTotal(vendor.getPackagesTotal())
        .withPackagesSelected(vendor.getPackagesSelected())
        .withSupportsCustomPackages(vendor.isCustomer())
        .withProviderToken(token != null ? new Token().withValue(token) : null))
      .withRelationships(EMPTY_PACKAGE_RELATIONSHIP);
  }
}
