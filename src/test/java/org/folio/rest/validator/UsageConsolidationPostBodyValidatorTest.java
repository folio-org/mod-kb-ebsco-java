package org.folio.rest.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rest.validator.uc.UsageConsolidationPostBodyValidator;

public class UsageConsolidationPostBodyValidatorTest {

  private final UsageConsolidationPostBodyValidator validator = new UsageConsolidationPostBodyValidator();

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void shouldNotThrowExceptionWhenStartMonthIsNull() {
    UCSettingsPostRequest postRequest = new UCSettingsPostRequest()
      .withData(new UCSettingsPostDataAttributes()
        .withAttributes(new UCSettingsDataAttributes()
        .withCurrency("aaa")
        .withStartMonth(null)));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPlatformIsNull() {
    UCSettingsPostRequest postRequest = new UCSettingsPostRequest()
      .withData(new UCSettingsPostDataAttributes()
        .withAttributes(new UCSettingsDataAttributes()
          .withCurrency("aaa")
          .withPlatformType(null)));
    validator.validate(postRequest);
  }
}
