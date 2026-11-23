package org.folio.rest.validator.resources;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.util.ResourcesTestUtil;
import org.junit.jupiter.api.Test;

class CustomResourcePutBodyValidatorTest {

  private final CustomResourcePutBodyValidator validator =
    new CustomResourcePutBodyValidator(new ResourceCommonAttributesValidator(new CustomLabelsProperties(50, 100)));

  @Test
  void shouldNotThrowExceptionWhenUrlIsValidFormatForCustomResource() {
    validator.validate(ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("https://hello")));
  }

  @Test
  void shouldThrowExceptionWhenUrlIsInvalidFormatForCustomResource() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("hello"));

    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }

  @Test
  void shouldThrowExceptionWhenCvgStmtExceedsLengthForCustomResource() {
    var request = ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement(RandomStringUtils.insecure().nextAlphanumeric(251)));

    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }

  @Test
  void shouldNotThrowExceptionWhenCvgStmtIsValidForCustomResource() {
    validator.validate(ResourcesTestUtil.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("my test coverage statement")));
  }
}
