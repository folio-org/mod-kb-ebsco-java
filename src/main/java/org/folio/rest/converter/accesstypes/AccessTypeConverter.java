package org.folio.rest.converter.accesstypes;

import static org.apache.commons.lang3.StringUtils.isAllBlank;

import static org.folio.db.RowSetUtils.fromDate;
import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;
import static org.folio.db.RowSetUtils.toUUID;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.UserDisplayInfo;

public class AccessTypeConverter {

  private AccessTypeConverter() {

  }

  @Component
  public static class FromDb implements Converter<DbAccessType, AccessType> {

    @Override
    public AccessType convert(@NotNull DbAccessType source) {
      AccessType accessType = new AccessType()
        .withId(source.getId().toString())
        .withType(AccessType.Type.ACCESS_TYPES)
        .withAttributes(new AccessTypeDataAttributes()
          .withName(source.getName())
          .withDescription(source.getDescription())
          .withCredentialsId(source.getCredentialsId().toString()))
        .withUsageNumber(source.getUsageNumber())
        .withCreator(new UserDisplayInfo()
          .withFirstName(source.getCreatedByFirstName())
          .withLastName(source.getCreatedByLastName())
          .withMiddleName(source.getCreatedByMiddleName()))
        .withMetadata(new Metadata()
          .withCreatedByUserId(fromUUID(source.getCreatedByUserId()))
          .withCreatedByUsername(source.getCreatedByUserName())
          .withCreatedDate(toDate(source.getCreatedDate()))
          .withUpdatedByUserId(fromUUID(source.getUpdatedByUserId()))
          .withUpdatedByUsername(source.getUpdatedByUserName())
          .withUpdatedDate(toDate(source.getUpdatedDate())));
      if (!isAllBlank(source.getUpdatedByFirstName(), source.getUpdatedByLastName(), source.getUpdatedByMiddleName())) {
        accessType
          .withUpdater(new UserDisplayInfo()
            .withFirstName(source.getUpdatedByFirstName())
            .withLastName(source.getUpdatedByLastName())
            .withMiddleName(source.getUpdatedByMiddleName()));
      }
      return accessType;
    }

  }

  @Component
  public static class ToDb implements Converter<AccessType, DbAccessType> {

    @Override
    public DbAccessType convert(@NotNull AccessType source) {
      AccessTypeDataAttributes attributes = source.getAttributes();
      var builder = DbAccessType.builder()
        .id(toUUID(source.getId()))
        .name(attributes.getName())
        .description(attributes.getDescription())
        .credentialsId(toUUID(attributes.getCredentialsId()));

      UserDisplayInfo creator = source.getCreator();
      if (creator != null) {
        builder
          .createdByFirstName(creator.getFirstName())
          .createdByMiddleName(creator.getMiddleName())
          .createdByLastName(creator.getLastName());
      }

      UserDisplayInfo updater = source.getUpdater();
      if (updater != null) {
        builder
          .updatedByFirstName(updater.getFirstName())
          .updatedByMiddleName(updater.getMiddleName())
          .updatedByLastName(updater.getLastName());
      }

      Metadata metadata = source.getMetadata();
      if (metadata != null) {
        builder
          .createdDate(fromDate(metadata.getCreatedDate()))
          .createdByUserId(toUUID(metadata.getCreatedByUserId()))
          .createdByUserName(metadata.getCreatedByUsername())
          .updatedDate(fromDate(metadata.getUpdatedDate()))
          .updatedByUserId(toUUID(metadata.getUpdatedByUserId()))
          .updatedByUserName(metadata.getUpdatedByUsername());
      }
      return builder.build();
    }
  }
}
