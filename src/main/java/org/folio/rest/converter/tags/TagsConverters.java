package org.folio.rest.converter.tags;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import java.util.UUID;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.rest.jaxrs.model.TagDataAttributes;
import org.folio.rest.jaxrs.model.TagUniqueCollection;
import org.folio.rest.jaxrs.model.TagUniqueCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class TagsConverters {

  private TagsConverters() {
  }

  @Component
  public static class ToTags implements Converter<List<DbTag>, Tags> {

    @Override
    public Tags convert(@NonNull List<DbTag> source) {
      return new Tags().withTagList(mapItems(source, DbTag::getValue));
    }
  }

  @Component
  public static class ToTagCollection implements Converter<List<DbTag>, TagCollection> {

    private final Converter<DbTag, TagCollectionItem> tagConverter;

    public ToTagCollection(Converter<DbTag, TagCollectionItem> tagConverter) {
      this.tagConverter = tagConverter;
    }

    @Override
    public TagCollection convert(@NonNull List<DbTag> source) {
      return new TagCollection()
        .withData(mapItems(source, tagConverter::convert))
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(source.size()));
    }
  }

  @Component
  public static class ToUniqueCollectionItem implements Converter<String, TagUniqueCollectionItem> {

    @Override
    public TagUniqueCollectionItem convert(@NonNull String source) {
      return new TagUniqueCollectionItem()
        .withId(UUID.randomUUID().toString())
        .withType(RestConstants.TAGS_TYPE)
        .withAttributes(new TagDataAttributes().withValue(source));
    }
  }

  @Component
  public static class ToUniqueTagCollection implements Converter<List<String>, TagUniqueCollection> {

    private final Converter<String, TagUniqueCollectionItem> tagConverter;

    public ToUniqueTagCollection(Converter<String, TagUniqueCollectionItem> tagConverter) {
      this.tagConverter = tagConverter;
    }

    @Override
    public TagUniqueCollection convert(@NonNull List<String> source) {
      return new TagUniqueCollection()
        .withData(mapItems(source, tagConverter::convert))
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(source.size()));
    }
  }
}
