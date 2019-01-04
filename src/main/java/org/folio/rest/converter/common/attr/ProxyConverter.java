package org.folio.rest.converter.common.attr;

import java.util.Objects;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.rmapi.model.Proxy;

@Component
public class ProxyConverter implements Converter<Proxy, org.folio.rest.jaxrs.model.Proxy> {

  @Override
  public org.folio.rest.jaxrs.model.Proxy convert(@Nullable Proxy proxy) {
    if(Objects.isNull(proxy)){
      return null;
    }
    org.folio.rest.jaxrs.model.Proxy p = new org.folio.rest.jaxrs.model.Proxy();
    p.setId(proxy.getId());
    p.setInherited(proxy.getInherited());
    return p;
  }

}
