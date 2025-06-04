package org.folio.rest.converter.common.attr;

import java.util.Objects;
import org.folio.holdingsiq.model.ProxyUrl;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ProxyConverter implements Converter<ProxyUrl, org.folio.rest.jaxrs.model.ProxyUrl> {
  @Override
  public org.folio.rest.jaxrs.model.ProxyUrl convert(@Nullable ProxyUrl proxy) {
    if (Objects.isNull(proxy)) {
      return null;
    }
    org.folio.rest.jaxrs.model.ProxyUrl p = new org.folio.rest.jaxrs.model.ProxyUrl();
    p.setId(proxy.getId());
    p.setInherited(proxy.getInherited());
    p.setProxiedUrl(proxy.getProxiedUrl());
    return p;
  }
}
