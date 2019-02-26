package org.folio.rest.converter.providers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Vendors;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.util.RestConstants;

@Component
public class ProviderCollectionConverter implements Converter<Vendors, ProviderCollection> {

  @Autowired
  private ProvidersConverter providersConverter;

  @Override
  public ProviderCollection convert(@NonNull Vendors vendors) {
    List<Providers> providerList = vendors.getVendorList().stream()
      .map(providersConverter::convert)
      .collect(Collectors.toList());
    return new ProviderCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(vendors.getTotalResults()))
      .withData(providerList);
  }
}
