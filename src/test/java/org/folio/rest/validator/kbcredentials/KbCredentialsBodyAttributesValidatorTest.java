package org.folio.rest.validator.kbcredentials;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KbCredentialsBodyAttributesValidatorTest {

  private static final int NAME_LENGTH_MAX = 10;

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private final KbCredentialsBodyAttributesValidator validator =
    new KbCredentialsBodyAttributesValidator(NAME_LENGTH_MAX);

  @Test
  public void shouldThrowExceptionWhenNullName() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenEmptyName() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("")
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsLongerThanMaxLength() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("0".repeat(NAME_LENGTH_MAX + 1))
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenNullApiKey() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid apiKey");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenEmptyApiKey() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid apiKey");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("")
      .withCustomerId("custId")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenNullCustomerId() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid customerId");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenEmptyCustomerId() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid customerId");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("")
      .withUrl("http://example.com");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenNullUrl() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid url");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("custId");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenEmptyUrl() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid url");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("");

    validator.validateAttributes(attributes);
  }

  @Test
  public void shouldThrowExceptionWhenInvalidUrl() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid url");
    KbCredentialsDataAttributes attributes = new KbCredentialsDataAttributes()
      .withName("name")
      .withApiKey("key")
      .withCustomerId("custId")
      .withUrl("ht:/dda");

    validator.validateAttributes(attributes);
  }
}
