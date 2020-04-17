package org.folio.rest.validator;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

public class CustomLabelsPutBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private CustomLabelsPutBodyValidator validator = new CustomLabelsPutBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutBody() {
    CustomLabelPutRequest putRequest = null;
    validator.validate(putRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyBody() {
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest();
    putRequest.withData(null);
    validator.validate(putRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutData() {
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest().withData(null);
    validator.validate(putRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPutData() {
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(Collections.singletonList(new CustomLabel()));
    validator.validate(putRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenInvalidId() {
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(Collections.singletonList(new CustomLabel()
        .withAttributes(new CustomLabelDataAttributes().withId(6))));
    validator.validate(putRequest);
  }

  @Test
  public void shouldThrowExceptionWhenIdIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label id");
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(Collections.singletonList(
        new CustomLabel().withAttributes(new CustomLabelDataAttributes().withId(null))));
    validator.validate(putRequest);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label Name");
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
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
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
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
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
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
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
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
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
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
