package org.folio.rest.converter.providers;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Vendors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProviderCollectionConverter implements Converter<Vendors, ProviderCollection> {

  @Autowired
  private ProvidersConverter providersConverter;

  @Override
  public ProviderCollection convert(Vendors vendors) {
    List<Providers> providerList = vendors.getVendorList().stream()
      .map(providersConverter::convert)
      .collect(Collectors.toList());
    return new ProviderCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(vendors.getTotalResults()))
      .withData(providerList);
  }
}
