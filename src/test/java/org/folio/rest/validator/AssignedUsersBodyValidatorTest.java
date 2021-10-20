package org.folio.rest.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserDataAttributes;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;

public class AssignedUsersBodyValidatorTest {

  private static final int MAX_CHARACTERS = 200;
  private final AssignedUsersBodyValidator validator = new AssignedUsersBodyValidator();

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoData() {
    AssignedUserPostRequest request = new AssignedUserPostRequest().withData(null);
    validator.validate(request.getData());
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenUserNameIsEmpty() {
    AssignedUser data = new AssignedUser().withAttributes(
      new AssignedUserDataAttributes()
        .withUserName(""));
    validator.validate(data);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenLastNameIsBlank() {
    AssignedUser data = new AssignedUser().withAttributes(
      new AssignedUserDataAttributes()
        .withUserName("userName")
        .withLastName(" "));
    validator.validate(data);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenFirstNameIsTooLong() {
    AssignedUser data = new AssignedUser().withAttributes(
      new AssignedUserDataAttributes()
        .withUserName("userName")
        .withFirstName("n".repeat(MAX_CHARACTERS + 1))
        .withLastName("lastName")
        .withPatronGroup("patronGroup"));
    validator.validate(data);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPatronGroupIsEmpty() {
    AssignedUser data = new AssignedUser().withAttributes(
      new AssignedUserDataAttributes()
        .withUserName("userName")
        .withLastName("lastName")
        .withPatronGroup(""));
    validator.validate(data);
  }

  @Test
  public void shouldNotThrowExceptionWhenFirstNameIsNull() {
    AssignedUser data = new AssignedUser().withAttributes(
      new AssignedUserDataAttributes()
        .withUserName("userName")
        .withFirstName(null)
        .withLastName("lastName")
        .withPatronGroup("patronGroup"));
    validator.validate(data);
  }
}
