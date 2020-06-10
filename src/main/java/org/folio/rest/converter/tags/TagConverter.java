package org.folio.rest.converter.tags;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.repository.RecordType;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.rest.jaxrs.model.TagDataAttributes;
import org.folio.rest.jaxrs.model.TagRelationship;
import org.folio.rest.util.RestConstants;

@Component
public class TagConverter implements Converter<DbTag, TagCollectionItem> {

  private static final Map<RecordType, String> RECORD_TYPES = new EnumMap<>(RecordType.class);

  static {
    RECORD_TYPES.put(RecordType.PACKAGE, RestConstants.PACKAGES_TYPE);
    RECORD_TYPES.put(RecordType.PROVIDER, RestConstants.PROVIDERS_TYPE);
    RECORD_TYPES.put(RecordType.TITLE, RestConstants.TITLES_TYPE);
    RECORD_TYPES.put(RecordType.RESOURCE, RestConstants.RESOURCES_TYPE);
  }

  @Override
  public TagCollectionItem convert(@NonNull DbTag source) {
    return new TagCollectionItem()
      .withType(RestConstants.TAGS_TYPE)
      .withId(RowSetUtils.fromUUID(source.getId()))
      .withAttributes(createAttributes(source))
      .withRelationships(createRelationships(source));
  }

  private TagDataAttributes createAttributes(DbTag source) {
    return new TagDataAttributes().withValue(source.getValue());
  }

  private TagRelationship createRelationships(DbTag source) {
    return new TagRelationship().withRecord(
            new HasOneRelationship().withData(
                new RelationshipData()
                  .withId(source.getRecordId())
                  .withType(RECORD_TYPES.get(source.getRecordType()))));
  }

}
