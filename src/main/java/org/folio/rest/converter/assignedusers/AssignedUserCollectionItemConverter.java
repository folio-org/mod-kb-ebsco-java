package org.folio.rest.converter.assignedusers;

import static org.folio.rest.jaxrs.model.AssignedUser.Type.ASSIGNED_USERS;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserDataAttributes;
import org.folio.rest.jaxrs.model.AssignedUserId;

public class AssignedUserCollectionItemConverter {

  private AssignedUserCollectionItemConverter() {

  }

  @Component
  public static class FromDb implements Converter<DbAssignedUser, AssignedUser> {

    @Override
    public AssignedUser convert(DbAssignedUser source) {
      return new AssignedUser()
        .withId(source.getId().toString())
        .withType(ASSIGNED_USERS)
        .withAttributes(new AssignedUserDataAttributes()
          .withCredentialsId(source.getCredentialsId().toString())
          .withUserName(source.getUsername())
          .withFirstName(source.getFirstName())
          .withMiddleName(source.getMiddleName())
          .withLastName(source.getLastName())
          .withPatronGroup(source.getPatronGroup())
        );
    }
  }

  @Component
  public static class toAssignedUserId implements Converter<DbAssignedUser, AssignedUserId> {

    @Override
    public AssignedUserId convert(DbAssignedUser source) {
      return new AssignedUserId()
        .withId(source.getId().toString())
        .withCredentialsId(source.getCredentialsId().toString());
    }
  }

  @Component
  public static class ToDb implements Converter<AssignedUserId, DbAssignedUser> {

    @Override
    public DbAssignedUser convert(AssignedUserId source) {
      return DbAssignedUser.builder()
        .id(UUID.fromString(source.getId()))
        .credentialsId(UUID.fromString(source.getCredentialsId()))
        .build();
    }
  }

}
