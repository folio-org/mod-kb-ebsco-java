package org.folio.rest.converter.tags;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.tag.Tag;

public class TagConverter implements Converter<Tag, TagCollectionItem> {

  @Override
  public TagCollectionItem convert(@NonNull Tag source) {
    return null;
  }

}
