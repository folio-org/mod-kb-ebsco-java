package org.folio.rest.converter.assignedusers;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;

public class AssignedUserCollectionItemConverter {

  private AssignedUserCollectionItemConverter() {

  }

  @Component
  public static class FromDb implements Converter<DbAssignedUser, AssignedUser> {

    @Override
    public AssignedUser convert(DbAssignedUser source) {
      return new AssignedUser()
        .withId(source.getId().toString());
    }
  }

  @Component
  public static class ToDb implements Converter<AssignedUser, DbAssignedUser> {

    @Override
    public DbAssignedUser convert(AssignedUser source) {
      return DbAssignedUser.builder()
        .id(UUID.fromString(source.getId()))
        .build();
    }
  }

}
