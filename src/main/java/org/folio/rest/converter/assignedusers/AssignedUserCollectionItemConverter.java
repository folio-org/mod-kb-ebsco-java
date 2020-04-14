package org.folio.rest.converter.assignedusers;

import static org.folio.rest.jaxrs.model.AssignedUser.Type.ASSIGNED_USERS;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserDataAttributes;

public class AssignedUserCollectionItemConverter {

  private AssignedUserCollectionItemConverter() {

  }

  @Component
  public static class FromDb implements Converter<DbAssignedUser, AssignedUser> {

    @Override
    public AssignedUser convert(DbAssignedUser source) {
      return new AssignedUser()
        .withId(source.getId())
        .withType(ASSIGNED_USERS)
        .withAttributes(new AssignedUserDataAttributes()
          .withCredentialsId(source.getCredentialsId())
          .withUserName(source.getUsername())
          .withFirstName(source.getFirstName())
          .withMiddleName(source.getMiddleName())
          .withLastName(source.getLastName())
          .withPatronGroup(source.getPatronGroup())
        );
    }
  }

  @Component
  public static class ToDb implements Converter<AssignedUser, DbAssignedUser> {

    @Override
    public DbAssignedUser convert(AssignedUser source) {
      AssignedUserDataAttributes attributes = source.getAttributes();
      return DbAssignedUser.builder()
        .id(source.getId())
        .credentialsId(attributes.getCredentialsId())
        .username(attributes.getUserName())
        .firstName(attributes.getFirstName())
        .middleName(attributes.getMiddleName())
        .lastName(attributes.getLastName())
        .patronGroup(attributes.getPatronGroup())
        .build();
    }
  }

}
