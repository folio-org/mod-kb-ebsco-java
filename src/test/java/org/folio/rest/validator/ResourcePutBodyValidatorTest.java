package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ResourcePutBodyValidatorTest {
  private final ResourcePutBodyValidator validator = new ResourcePutBodyValidator();
  
  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  
  @Test
  public void shouldThrowExceptionWhenNameIsBlankForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("")), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenNameExceedsLengthForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName(RandomStringUtils.randomAlphanumeric(500))), true);
  }
  
  @Test
  public void shouldNotThrowExceptionWhenNameIsNotBlankAndWithinLimitForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName(RandomStringUtils.randomAlphanumeric(399))
          .withPublicationType(PublicationType.AUDIOBOOK)), true);
  }

  @Test
  public void shouldNotThrowExceptionWhenPublicationTypeIsNotBlankForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenPublisherNameExceedsLengthForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withPublisherName(RandomStringUtils.randomAlphanumeric(251))), true);
  }
  
  @Test
  public void shouldNotThrowExceptionWhenPublisherNameIsWithinLimitForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withPublisherName(RandomStringUtils.randomAlphanumeric(249))), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenEditionExceedsLengthForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withEdition(RandomStringUtils.randomAlphanumeric(251))), true);
  }
  
  @Test
  public void shouldNotThrowExceptionWhenEditionIsWithinLimitForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withEdition(RandomStringUtils.randomAlphanumeric(249))), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenDescriptionExceedsLengthForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withDescription(RandomStringUtils.randomAlphanumeric(1501))), true);
  }
  
  @Test
  public void shouldNotThrowExceptionWhenDescriptionIsWithinLimitForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withDescription(RandomStringUtils.randomAlphanumeric(249))), true);
  }
  
  @Test
  public void shouldNotThrowExceptionWhenUrlIsValidFormatForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withUrl("https://hello")), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenUrlIsInvalidFormatForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withUrl("hello")), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenCvgStmtExceedsLengthForACustomResource() {
    expectedEx.expect(InputValidationException.class);
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withCoverageStatement(RandomStringUtils.randomAlphanumeric(251))), true);
  }
  
  @Test
  public void shouldNotThrowExceptionWhenCvgStmtIsValidForACustomResource() {
    validator.validate(ResourcesTestData.getResourcePutRequest(
        new ResourceDataAttributes()
          .withIsSelected(true)
          .withName("some name")
          .withPublicationType(PublicationType.AUDIOBOOK)
          .withCoverageStatement("my test coverage statement")), true);
  }
  
  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndIsHiddenIsTrue() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
      .withIsSelected(false)
      .withVisibilityData(new VisibilityData()
        .withIsHidden(true))), false);
  }
  
  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndCvgStmtIsNotNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
      .withIsSelected(false)
      .withCoverageStatement("hello")), false);
  }
  
  @Test
  public void shouldThrowExceptionWhenResourceIsNotSelectedAndCustomEmbargoIsNotNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Resource cannot be updated unless added to holdings");
    validator.validate(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
      .withIsSelected(false)
      .withCustomEmbargoPeriod(new EmbargoPeriod().withEmbargoUnit(EmbargoUnit.DAYS))), false);
  }
}
