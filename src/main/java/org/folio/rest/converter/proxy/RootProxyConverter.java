package org.folio.rest.converter.proxy;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyData;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RootProxyConverter implements Converter<RootProxyCustomLabels, RootProxy> {

  @Override
  public RootProxy convert(@NonNull RootProxyCustomLabels proxy) {
    return new org.folio.rest.jaxrs.model.RootProxy()
      .withData(new RootProxyData()
        .withId(RootProxyData.Id.ROOT_PROXY)
        .withType(RootProxyData.Type.ROOT_PROXIES)
        .withAttributes(new RootProxyDataAttributes()
          .withId(RootProxyDataAttributes.Id.ROOT_PROXY)
          .withProxyTypeId(proxy.getProxy().getId())))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
