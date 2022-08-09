package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rmapi.result.ResourceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ResourceConverter implements Converter<ResourceResult, Resource> {

  @Autowired
  private Converter<ResourceResult, List<Resource>> resultListConverter;

  @Override
  public Resource convert(@NonNull ResourceResult resourceResult) {
    Resource resource = requireNonNull(resultListConverter.convert(resourceResult)).get(0);
    resource.getData().getAttributes().setTags(resourceResult.getTags());
    return resource;
  }
}
