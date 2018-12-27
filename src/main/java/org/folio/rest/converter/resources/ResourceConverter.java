package org.folio.rest.converter.resources;

import java.util.List;

import org.folio.rest.jaxrs.model.Resource;
import org.folio.rmapi.result.ResourceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceConverter implements Converter<ResourceResult, Resource> {
  @Autowired
  private Converter<ResourceResult, List<Resource>> resultListConverter;

  @Override
  public Resource convert(ResourceResult resourceResult) {
    return resultListConverter.convert(resourceResult).get(0);
  }
}
