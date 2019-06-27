package org.folio.rest.validator;

import org.junit.Test;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostData;
import org.folio.rest.jaxrs.model.PackagePostDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;

public class PackagePostValidatorTest {

  private PackagesPostBodyValidator validator = new PackagesPostBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPostBody(){
    PackagePostRequest postRequest = null;
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyBody(){
    PackagePostRequest postRequest = new PackagePostRequest();
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPostData(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(null);
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostData(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData());
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostDataAttributes(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPostDataAttributesNameIsEmpty(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("")));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPostDataAttributesTypeIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name").withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage())));
    validator.validate(postRequest);
  }
  @Test(expected = InputValidationException.class)

  public void shouldThrowExceptionWhenPostDataAttributeCustomCoverageWithEmptyBeginDate(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage("2003-11-01"))
      ));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsInvalidFormat(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.E_BOOK)
          .withCustomCoverage(new Coverage()
            .withBeginCoverage("-01"))));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPostDataAttributeCustomCoverageEndDateIsInvalidFormat(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.E_BOOK)
          .withCustomCoverage(new Coverage()
            .withBeginCoverage("2003-11-01")
            .withEndCoverage("-01"))));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsBeforeEndDate(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-12-01")
          .withEndCoverage("2003-11-01"))
      ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidDates(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-11-01")
          .withEndCoverage("2003-12-01"))
      ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithEmptyDates(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
      ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsEmpty() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.E_BOOK)
          .withCustomCoverage(new Coverage()
            .withBeginCoverage(""))));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsEmpty(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-11-01")
          .withEndCoverage(""))
      ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-11-01")
          .withEndCoverage(null))
      ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsNullw(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(null))
      ));
    validator.validate(postRequest);
  }

}
