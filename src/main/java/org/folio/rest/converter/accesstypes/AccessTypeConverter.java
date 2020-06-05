package org.folio.rest.converter.accesstypes;

import static org.apache.commons.lang3.StringUtils.isAllBlank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

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
          .withCredentialsId(source.getCredentialsId()))
        .withUsageNumber(source.getUsageNumber())
        .withCreator(new UserDisplayInfo()
          .withFirstName(source.getCreatedByFirstName())
          .withLastName(source.getCreatedByLastName())
          .withMiddleName(source.getCreatedByMiddleName()))
        .withMetadata(new Metadata()
          .withCreatedByUserId(source.getCreatedByUserId())
          .withCreatedByUsername(source.getCreatedByUsername())
          .withCreatedDate(toDate(source.getCreatedDate()))
          .withUpdatedByUserId(source.getUpdatedByUserId())
          .withUpdatedByUsername(source.getUpdatedByUsername())
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

    private Date toDate(LocalDateTime date) {
      return date != null ? Date.from(date.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }
  }

  @Component
  public static class ToDb implements Converter<AccessType, DbAccessType> {

    @Override
    public DbAccessType convert(@NotNull AccessType source) {
      AccessTypeDataAttributes attributes = source.getAttributes();
      DbAccessType.DbAccessTypeBuilder builder = DbAccessType.builder()
        .id(UUID.fromString(source.getId()))
        .name(attributes.getName())
        .description(attributes.getDescription())
        .credentialsId(attributes.getCredentialsId());

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
          .createdDate(toInstant(metadata.getCreatedDate()))
          .createdByUserId(metadata.getCreatedByUserId())
          .createdByUsername(metadata.getCreatedByUsername())
          .updatedDate(toInstant(metadata.getUpdatedDate()))
          .updatedByUserId(metadata.getUpdatedByUserId())
          .updatedByUsername(metadata.getUpdatedByUsername());
      }
      return builder.build();
    }

    private LocalDateTime toInstant(Date date) {
      return date != null ? LocalDateTime.from(date.toInstant()) : null;
    }
  }
}
