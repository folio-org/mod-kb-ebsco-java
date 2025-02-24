package org.folio.rest.converter.accesstypes;

import static org.folio.db.RowSetUtils.fromDate;
import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;
import static org.folio.db.RowSetUtils.toUUID;

import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.Metadata;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class AccessTypeConverter {

  private AccessTypeConverter() {

  }

  @Component
  public static class FromDb implements Converter<DbAccessType, AccessType> {

    @Override
    public AccessType convert(@NonNull DbAccessType source) {
      return new AccessType()
        .withId(source.getId().toString())
        .withType(AccessType.Type.ACCESS_TYPES)
        .withAttributes(new AccessTypeDataAttributes()
          .withName(source.getName())
          .withDescription(source.getDescription())
          .withCredentialsId(source.getCredentialsId().toString()))
        .withUsageNumber(source.getUsageNumber())
        .withMetadata(new Metadata()
          .withCreatedByUserId(fromUUID(source.getCreatedByUserId()))
          .withCreatedDate(toDate(source.getCreatedDate()))
          .withUpdatedByUserId(fromUUID(source.getUpdatedByUserId()))
          .withUpdatedDate(toDate(source.getUpdatedDate())));
    }

  }

  @Component
  public static class ToDb implements Converter<AccessType, DbAccessType> {

    @Override
    public DbAccessType convert(@NonNull AccessType source) {
      AccessTypeDataAttributes attributes = source.getAttributes();
      var builder = DbAccessType.builder()
        .id(toUUID(source.getId()))
        .name(attributes.getName())
        .description(attributes.getDescription())
        .credentialsId(toUUID(attributes.getCredentialsId()));

      Metadata metadata = source.getMetadata();
      if (metadata != null) {
        builder
          .createdDate(fromDate(metadata.getCreatedDate()))
          .createdByUserId(toUUID(metadata.getCreatedByUserId()))
          .updatedDate(fromDate(metadata.getUpdatedDate()))
          .updatedByUserId(toUUID(metadata.getUpdatedByUserId()));
      }
      return builder.build();
    }
  }
}
