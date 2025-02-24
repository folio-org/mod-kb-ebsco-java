package org.folio.rest.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.junit.Test;

public class ResourcePutBodyValidatorTest {

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
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("hello"));

    assertThrows(InputValidationException.class, () -> validator.validate(request, true));
  }

  @Test
  public void shouldThrowExceptionWhenCvgStmtExceedsLengthForCustomResource() {
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement(RandomStringUtils.insecure().nextAlphanumeric(251)));

    assertThrows(InputValidationException.class, () -> validator.validate(request, true));
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
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withVisibilityData(new VisibilityData().withIsHidden(true)));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndCvgStmtIsNotNull() {
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCoverageStatement("hello"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedFieldIsNotNull() {
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField1("not null"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
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
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUserDefinedField1(RandomStringUtils.insecure().nextAlphanumeric(101)));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Invalid userDefinedField1", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndCustomEmbargoIsNotNull() {
    var request = ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCustomEmbargoPeriod(new EmbargoPeriod().withEmbargoUnit(EmbargoUnit.DAYS)));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  private void testUserDefinedFieldValidation(ResourcePutDataAttributes value) {
    var request = ResourcesTestData.getResourcePutRequest(value);

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }
}
