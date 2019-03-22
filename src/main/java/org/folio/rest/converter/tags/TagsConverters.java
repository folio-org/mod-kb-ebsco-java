package org.folio.rest.converter.tags;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.TagCollectionItem;
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
      return new Tags().withTagList(mapItems(source, Tag::getValue));
    }

  }

  @Component
  public static class ToTagCollection implements Converter<List<Tag>, TagCollection> {

    @Autowired
    private Converter<Tag, TagCollectionItem> tagConverter;

    @Override
    public TagCollection convert(@NonNull List<Tag> source) {
      return new TagCollection()
                  .withData(mapItems(source, tagConverter::convert))
                  .withJsonapi(RestConstants.JSONAPI)
                  .withMeta(new MetaTotalResults().withTotalResults(source.size()));
    }
    
  }

  private static <T, R> List<R> mapItems(List<T> source, Function<? super T, ? extends R> mapper) {
    return source.stream().map(mapper).collect(Collectors.toList());
  }
}
