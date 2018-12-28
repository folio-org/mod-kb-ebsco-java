package org.folio.rest.converter.proxy;

import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyData;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.RootProxyCustomLabels;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RootProxyConverter implements Converter<RootProxyCustomLabels, RootProxy> {
  private static final String ROOT_PROXY_ID = "root-proxy";
  private static final String ROOT_PROXY_TYPE = "rootProxies";

  @Override
  public RootProxy convert(RootProxyCustomLabels proxy) {
    return new org.folio.rest.jaxrs.model.RootProxy()
      .withData(new RootProxyData()
        .withId(ROOT_PROXY_ID)
        .withType(ROOT_PROXY_TYPE)
        .withAttributes(new RootProxyDataAttributes()
          .withId(ROOT_PROXY_ID)
          .withProxyTypeId(proxy.getProxy().getId())))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
