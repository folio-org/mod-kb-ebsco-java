package org.folio.rest.validator;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;
import org.junit.Test;

public class CustomLabelsPutBodyValidatorTest {

  private final CustomLabelsPutBodyValidator validator =
    new CustomLabelsPutBodyValidator(new CustomLabelsProperties(50, 100));

  @Test
  public void shouldThrowExceptionWhenIdTooSmall() {
    assertInvalidCustomLabelId(0);
  }

  @Test
  public void shouldThrowExceptionWhenIdTooBig() {
    assertInvalidCustomLabelId(6);
  }

  @Test
  public void shouldThrowExceptionWhenIdIsNull() {
    assertInvalidCustomLabelId(null);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes().withId(1)
            .withDisplayLabel(insecure().nextAlphanumeric(51)))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid Custom Label Name", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenPublicationFinderIsNull() {
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(new CustomLabel().withAttributes(
        new CustomLabelDataAttributes()
          .withId(1)
          .withDisplayLabel(insecure().nextAlphanumeric(40))
          .withDisplayOnFullTextFinder(false)
          .withDisplayOnPublicationFinder(null))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid Publication Finder", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenFullTextFinderIsNull() {
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(new CustomLabel().withAttributes(
        new CustomLabelDataAttributes()
          .withId(1)
          .withDisplayLabel(insecure().nextAlphanumeric(40))
          .withDisplayOnFullTextFinder(null)
          .withDisplayOnPublicationFinder(false))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid Full Text Finder", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenIdsDuplicating() {
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Arrays.asList(
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(1)
            .withDisplayLabel(insecure().nextAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false)),
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(1)
            .withDisplayLabel(insecure().nextAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertEquals("Invalid request body", exception.getMessage());
  }

  @Test
  public void shouldNotFallWhenPutBodyRequestIsValid() {
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Arrays.asList(
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(1)
            .withDisplayLabel(insecure().nextAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false)),
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(5)
            .withDisplayLabel(insecure().nextAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false))));
    validator.validate(request);
  }

  private void assertInvalidCustomLabelId(Integer id) {
    CustomLabelsPutRequest putRequest = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(new CustomLabel()
        .withAttributes(new CustomLabelDataAttributes().withId(id))));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(putRequest));
    assertEquals("Invalid Custom Label id", exception.getMessage());
  }
}
