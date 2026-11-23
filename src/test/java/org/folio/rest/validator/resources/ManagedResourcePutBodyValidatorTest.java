package org.folio.rest.validator.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.Visibility;
import org.folio.util.ResourcesTestUtil;
import org.junit.jupiter.api.Test;

class ManagedResourcePutBodyValidatorTest {

  private final ManagedResourcePutBodyValidator validator =
    new ManagedResourcePutBodyValidator(new ResourceCommonAttributesValidator(new CustomLabelsProperties(50, 100)));

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndCategoryIsHidden() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withVisibility(List.of(
          new Visibility().withCategory(Visibility.Category.PF).withHidden(false),
          new Visibility().withCategory(Visibility.Category.MARC).withHidden(true))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid visibility", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndVisibilityIsSentEvenIfNoCategoryIsHidden() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withVisibility(List.of(new Visibility().withCategory(Visibility.Category.PF).withHidden(false))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid visibility", exception.getMessage());
  }

  @Test
  void shouldNotThrowExceptionWhenResourceIsNotSelectedAndVisibilityIsEmpty() {
    validator.validate(ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withVisibility(List.of())));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndCvgStmtIsNotNull() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCoverageStatement("hello"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid coverageStatement", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedFieldIsNotNull() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField1("not null"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid userDefinedField1", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField2IsNotNull() {
    testUserDefinedFieldValidation("userDefinedField2",
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField2("not null"));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField3IsNotNull() {
    testUserDefinedFieldValidation("userDefinedField3",
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField3("not null"));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField4IsNotNull() {
    testUserDefinedFieldValidation("userDefinedField4",
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withUserDefinedField4("not null"));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndUserDefinedField5IsNotNull() {
    testUserDefinedFieldValidation("userDefinedField5",
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

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid userDefinedField1", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNotSelectedAndCustomEmbargoIsNotNull() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(false)
        .withCustomEmbargoPeriod(new EmbargoPeriod().withEmbargoUnit(EmbargoUnit.DAYS)));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid customEmbargoPeriod.embargoUnit", exception.getMessage());
  }

  private void testUserDefinedFieldValidation(String field, ResourcePutDataAttributes value) {
    var request = ResourcesTestUtil.getResourcePutRequest(value);

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid " + field, exception.getMessage());
  }
}
