package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ResourcePutBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private final ResourcePutBodyValidator validator = new ResourcePutBodyValidator(new CustomLabelsProperties(50, 100));

  @Test
  public void shouldNotThrowExceptionWhenUrlIsValidFormatForCustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("https://hello")), true);
  }

  @Test
  public void shouldThrowExceptionWhenUrlIsInvalidFormatForCustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("hello")), true);
  }

  @Test
  public void shouldThrowExceptionWhenCvgStmtExceedsLengthForCustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement(RandomStringUtils.randomAlphanumeric(251))), true);
  }

  @Test
  public void shouldNotThrowExceptionWhenCvgStmtIsValidForCustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("my test coverage statement")), true);
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndIsHiddenIsTrue() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withVisibilityData(new VisibilityData().withIsHidden(true))), false);
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndCvgStmtIsNotNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCoverageStatement("hello")), false);
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedFieldIsNotNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField1("not null")), false);
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField2IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField2("not null"));
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField3IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField3("not null"));
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField4IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField4("not null"));
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField5IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField5("not null"));
  }

  @Test
  public void shouldThrowExceptionWhenUserDefinedFieldIsLongerThanAllowed() {
    expectedEx.expect(InputValidationException.class);

    expectedEx.expectMessage("Invalid userDefinedField1");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUserDefinedField1(RandomStringUtils.randomAlphanumeric(101))), false);
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndCustomEmbargoIsNotNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCustomEmbargoPeriod(new EmbargoPeriod().withEmbargoUnit(EmbargoUnit.DAYS))), false);
  }

  private void testUserDefinedFieldValidation(ResourcePutDataAttributes value) {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(value), false);
  }
}
