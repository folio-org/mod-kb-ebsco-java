package org.folio.rest.converter.users;

import jakarta.validation.constraints.NotNull;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.service.users.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class UserConverter {

  private UserConverter() {

  }

  @Component
  public static class FromAssignedUser implements Converter<AssignedUserId, User> {

    @Override
    public User convert(@NotNull AssignedUserId source) {
      return User.builder()
        .id(source.getId())
        .build();
    }
  }
}
