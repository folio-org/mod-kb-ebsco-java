package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

public class CustomLabelsPutBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private CustomLabelsPutBodyValidator validator = new CustomLabelsPutBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutBody(){
    CustomLabelPutRequest putRequest = null;
    validator.validate(putRequest, 1);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyBody(){
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest();
    validator.validate(putRequest, 1);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutData(){
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(null);
    validator.validate(putRequest, 1);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPutData(){
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem());
    validator.validate(putRequest, 1);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenInvalidId(){
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withId(6)
        )
      );
    validator.validate(putRequest, 1);
  }

  @Test
  public void shouldThrowExceptionWhenNotEqualId() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid identifier id");
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withId(3)
        )
      );
    validator.validate(putRequest, 1);
  }

  @Test
  public void shouldThrowExceptionWhenIdIsNull(){
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label id");
    CustomLabelPutRequest putRequest = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withId(null)
        )
      );
    validator.validate(putRequest, 1);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Custom Label Name");
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withId(1)
          .withDisplayLabel(RandomStringUtils.randomAlphanumeric(51)))
      );
    validator.validate(request, 1);
  }

  @Test
  public void shouldThrowExceptionWhenPublicationFinderIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Publication Finder");
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
          .withId(1)
          .withDisplayOnFullTextFinder(false)
          .withDisplayOnPublicationFinder(null))
      );
    validator.validate(request, 1);
  }

  @Test
  public void shouldThrowExceptionWhenFullTextFinderIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid Full Text Finder");
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
          .withId(1)
          .withDisplayOnFullTextFinder(null)
          .withDisplayOnPublicationFinder(false))
      );
    validator.validate(request, 1);
  }

  @Test
  public void shouldNotFallWhenPutBodyRequestIsValid() {
    final CustomLabelPutRequest request = new CustomLabelPutRequest()
      .withData(new CustomLabelCollectionItem()
        .withAttributes(new CustomLabelDataAttributes()
          .withDisplayLabel(RandomStringUtils.randomAlphanumeric(40))
          .withId(1)
          .withDisplayOnFullTextFinder(false)
          .withDisplayOnPublicationFinder(false))
      );
    validator.validate(request, 1);
  }
}
