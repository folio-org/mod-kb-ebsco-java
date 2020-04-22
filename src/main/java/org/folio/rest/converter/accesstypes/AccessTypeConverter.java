package org.folio.rest.converter.accesstypes;

import static org.apache.commons.lang3.StringUtils.isAllBlank;

import java.time.Instant;
import java.util.Date;

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
        .withId(source.getId())
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

    private Date toDate(Instant date) {
      return date != null ? Date.from(date) : null;
    }
  }
}
