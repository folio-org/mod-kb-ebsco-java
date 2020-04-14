package org.folio.rest.converter.assignedusers;

import static org.folio.rest.jaxrs.model.AssignedUser.Type.ASSIGNED_USERS;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserDataAttributes;

@Component
public class AssignedUserCollectionItemConverter implements Converter<DbAssignedUser, AssignedUser> {

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
        .withPatronGroup(source.getPatronGroup()
        )
      );
  }
}
