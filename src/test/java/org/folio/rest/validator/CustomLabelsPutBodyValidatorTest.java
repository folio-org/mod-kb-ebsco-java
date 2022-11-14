package org.folio.rest.validator;

import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CustomLabelsPutBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private final CustomLabelsPutBodyValidator validator =
    new CustomLabelsPutBodyValidator(new CustomLabelsProperties(50, 100));

  @Test
  public void shouldThrowExceptionWhenInvalidId() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label id");
    CustomLabelsPutRequest putRequest = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(new CustomLabel()
        .withAttributes(new CustomLabelDataAttributes().withId(6))));
    validator.validate(putRequest);
  }

  @Test
  public void shouldThrowExceptionWhenIdIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label id");
    CustomLabelsPutRequest putRequest = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(
        new CustomLabel().withAttributes(new CustomLabelDataAttributes().withId(null))));
    validator.validate(putRequest);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label Name");
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes().withId(1)
            .withDisplayLabel(RandomStringUtils.randomAlphanumeric(51)))));
    validator.validate(request);
  }

  @Test
  public void shouldThrowExceptionWhenPublicationFinderIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Publication Finder");
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(new CustomLabel().withAttributes(
        new CustomLabelDataAttributes()
          .withId(1)
          .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
          .withDisplayOnFullTextFinder(false)
          .withDisplayOnPublicationFinder(null))));
    validator.validate(request);
  }

  @Test
  public void shouldThrowExceptionWhenFullTextFinderIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Full Text Finder");
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Collections.singletonList(new CustomLabel().withAttributes(
        new CustomLabelDataAttributes()
          .withId(1)
          .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
          .withDisplayOnFullTextFinder(null)
          .withDisplayOnPublicationFinder(false))));
    validator.validate(request);
  }

  @Test
  public void shouldThrowExceptionWhenIdsDuplicating() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid request body");
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Arrays.asList(
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(1)
            .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false)),
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(1)
            .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false))));
    validator.validate(request);
  }

  @Test
  public void shouldNotFallWhenPutBodyRequestIsValid() {
    final CustomLabelsPutRequest request = new CustomLabelsPutRequest()
      .withData(Arrays.asList(
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(1)
            .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false)),
        new CustomLabel().withAttributes(
          new CustomLabelDataAttributes()
            .withId(2)
            .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
            .withDisplayOnFullTextFinder(false)
            .withDisplayOnPublicationFinder(false))));
    validator.validate(request);
  }
}
