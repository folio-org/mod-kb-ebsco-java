package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rmapi.result.ResourceResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceConverter implements Converter<ResourceResult, Resource> {

  private final Converter<ResourceResult, List<Resource>> resultListConverter;

  public ResourceConverter(Converter<ResourceResult, List<Resource>> resultListConverter) {
    this.resultListConverter = resultListConverter;
  }

  @Override
  public Resource convert(ResourceResult resourceResult) {
    Resource resource = requireNonNull(resultListConverter.convert(resourceResult)).getFirst();
    resource.getData().getAttributes().setTags(resourceResult.getTags());
    return resource;
  }
}
