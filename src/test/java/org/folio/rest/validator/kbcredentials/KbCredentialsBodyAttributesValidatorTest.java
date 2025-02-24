package org.folio.rest.validator.kbcredentials;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.junit.Test;

public class KbCredentialsBodyAttributesValidatorTest {

  private static final int NAME_LENGTH_MAX = 10;

  private final KbCredentialsBodyAttributesValidator validator =
    new KbCredentialsBodyAttributesValidator(NAME_LENGTH_MAX);

  @Test
  public void shouldThrowExceptionWhenNullName() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid name", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenEmptyName() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("")
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid name", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenNameIsLongerThanMaxLength() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("0".repeat(NAME_LENGTH_MAX + 1))
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid name", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenNullApiKey() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid apiKey", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenEmptyApiKey() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid apiKey", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenNullCustomerId() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid customerId", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenEmptyCustomerId() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("")
      .withUrl("http://example.com");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid customerId", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenNullUrl() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("custId");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid url", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenEmptyUrl() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid url", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenInvalidUrl() {
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("ht:/dda");

    var exception = assertThrows(InputValidationException.class, () -> validator.validateAttributes(attributes));
    assertEquals("Invalid url", exception.getMessage());
  }
}
