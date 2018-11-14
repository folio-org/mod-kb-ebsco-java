package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.RootProxyData;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.RootProxyCustomLabels;


public class RootProxyConverter {
  private static final String ROOT_PROXY_ID = "root-proxy";
  private static final String ROOT_PROXY_TYPE = "rootProxies";

  public org.folio.rest.jaxrs.model.RootProxy convertRootProxy(RootProxyCustomLabels proxy) {
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
