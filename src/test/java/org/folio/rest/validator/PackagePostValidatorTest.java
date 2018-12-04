package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostData;
import org.folio.rest.jaxrs.model.PackagePostDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.junit.Test;

public class PackagePostValidatorTest {

  private PackagesPostBodyValidator validator = new PackagesPostBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenNoPostBody(){
    PackagePostRequest postRequest = null;
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenEmptyBody(){
    PackagePostRequest postRequest = new PackagePostRequest();
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenNoPostData(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(null);
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenEmptyPostData(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData());
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenEmptyPostDataAttributes(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenPostDataAttributesNameIsEmpty(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("")));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenPostDataAttributesTypeIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")));
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldTrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name").withContentType(ContentType.E_BOOK)
        .withCustomCoverage(new Coverage())));
    validator.validate(postRequest);
  }
  @Test(expected = InputValidationException.class)

  public void shouldTrowExceptionWhenPostDataAttributeCustomCoverageWithEmptyBeginDate(){
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
  public void shouldTrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsInvalidFormat(){
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
  public void shouldTrowExceptionWhenPostDataAttributeCustomCoverageEndDateIsInvalidFormat(){
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
  public void shouldTrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsBeforeEndDate(){
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
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageIsNull(){
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.E_BOOK)
        ));
    validator.validate(postRequest);
  }

  @Test
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageWithValidDates(){
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
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageWithEmptyDates(){
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
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsEmpty() {
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
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsEmpty(){
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
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsNull(){
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
  public void shouldNotTrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsNullw(){
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
