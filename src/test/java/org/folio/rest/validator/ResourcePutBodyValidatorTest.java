package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.util.ResourcesTestUtil;
import org.junit.jupiter.api.Test;

class ResourcePutBodyValidatorTest {

  private final ResourcePutBodyValidator validator = new ResourcePutBodyValidator(new CustomLabelsProperties(50, 100));

  @Test
  void shouldNotThrowExceptionWhenUrlIsValidFormatForCustomResource() {
    validator.validate(ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("https://hello")), true);
  }

  @Test
  void shouldThrowExceptionWhenUrlIsInvalidFormatForCustomResource() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("hello"));

    assertThrows(InputValidationException.class, () -> validator.validate(request, true));
  }

  @Test
  void shouldThrowExceptionWhenCvgStmtExceedsLengthForCustomResource() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement(RandomStringUtils.insecure().nextAlphanumeric(251)));

    assertThrows(InputValidationException.class, () -> validator.validate(request, true));
  }

  @Test
  void shouldNotThrowExceptionWhenCvgStmtIsValidForCustomResource() {
    validator.validate(ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("my test coverage statement")), true);
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndIsHiddenIsTrue() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withVisibilityData(new VisibilityData().withIsHidden(true)));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndCvgStmtIsNotNull() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCoverageStatement("hello"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedFieldIsNotNull() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField1("not null"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField2IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField2("not null"));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField3IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField3("not null"));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField4IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField4("not null"));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField5IsNotNull() {
    testUserDefinedFieldValidation(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField5("not null"));
  }

  @Test
  void shouldThrowExceptionWhenUserDefinedFieldIsLongerThanAllowed() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUserDefinedField1(RandomStringUtils.insecure().nextAlphanumeric(101)));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Invalid userDefinedField1", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndCustomEmbargoIsNotNull() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCustomEmbargoPeriod(new EmbargoPeriod().withEmbargoUnit(EmbargoUnit.DAYS)));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }

  private void testUserDefinedFieldValidation(ResourcePutDataAttributes value) {
    var request = ResourcesTestUtil.getResourcePutRequest(value);

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request, false));
    assertEquals("Resource cannot be updated unless added to holdings", exception.getMessage());
  }
}
