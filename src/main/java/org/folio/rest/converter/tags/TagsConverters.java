package org.folio.rest.converter.tags;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.RestConstants;
import org.folio.tag.Tag;

public class TagsConverters  {

  private TagsConverters() {
  }

  @Component
  public static class ToTags implements Converter<List<Tag>, Tags> {

    @Override
    public Tags convert(@NonNull List<Tag> source) {
      return new Tags().withTagList(toTagValues(source));
    }

  }

  @Component
  public static class ToTagCollection implements Converter<List<Tag>, TagCollection> {

    @Override
    public TagCollection convert(@NonNull List<Tag> source) {
      return new TagCollection()
                  .withData(toTagValues(source))
                  .withJsonapi(RestConstants.JSONAPI)
                  .withMeta(new MetaTotalResults().withTotalResults(source.size()));
    }

  }

  private static List<String> toTagValues(List<Tag> source) {
    return source.stream().map(Tag::getValue).collect(Collectors.toList());
  }

}
