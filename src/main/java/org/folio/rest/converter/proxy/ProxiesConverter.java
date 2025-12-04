package org.folio.rest.converter.proxy;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Proxies;
import org.folio.holdingsiq.model.ProxyWithUrl;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ProxyTypes;
import org.folio.rest.jaxrs.model.ProxyTypesData;
import org.folio.rest.jaxrs.model.ProxyTypesDataAttributes;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProxiesConverter implements Converter<Proxies, ProxyTypes> {

  @Override
  public ProxyTypes convert(Proxies proxies) {
    List<ProxyTypesData> providerList = mapItems(proxies.getProxyList(), this::convertProxy);

    return new ProxyTypes()
      .withMeta(new MetaTotalResults().withTotalResults(proxies.getProxyList().size()))
      .withJsonapi(RestConstants.JSONAPI).withData(providerList);
  }

  private ProxyTypesData convertProxy(ProxyWithUrl proxy) {
    return new ProxyTypesData()
      .withId(proxy.getId())
      .withType(ProxyTypesData.Type.PROXY_TYPES)
      .withAttributes(new ProxyTypesDataAttributes()
        .withId(proxy.getId())
        .withName(proxy.getName())
        .withUrlMask(proxy.getUrlMask())
      );
  }
}
