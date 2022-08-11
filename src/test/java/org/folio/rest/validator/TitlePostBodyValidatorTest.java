package org.folio.rest.validator;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Identifier;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.TitlePostData;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.folio.rest.jaxrs.model.TitlePostIncluded;
import org.folio.rest.jaxrs.model.TitlePostIncludedPackageId;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.junit.Test;

public class TitlePostBodyValidatorTest {

  private static final String TEXT_LONGER_THAN_250_CHARACTERS =
    "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Ae"
      + "nean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridicul"
      + "us mus. Donec quam felis, ultricies nec, pellentesque eu, pretium q";
  private static final String TEXT_LONGER_THAN_400_CHARACTERS =
    "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Ae"
      + "nean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridicul"
      + "us mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequ"
      + "at massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In"
      + " enim justo, rhoncus ut, imperdiet a,";
  private static final String TEXT_LONGER_THAN_1500_CHARACTERS =
    "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget d"
      + "olor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, "
      + "nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium "
      + "quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliqu"
      + "et nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vi"
      + "ae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dap"
      + "ibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo "
      + "ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapi"
      + "bus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius l"
      + "aoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitu"
      + "r ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus"
      + " eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque s"
      + "ed ipsum. Nam quam nunc, blandit vel, luctus pulvinar, hendrerit id, lorem. Maecen"
      + "as nec odio et ante tincidunt tempus. Donec vitae sapien ut libero venenatis fauci"
      + "bus. Nullam quis ante. Etiam sit amet orci eget eros faucibus tincidunt. Duis leo."
      + " Sed fringilla mauris sit amet nibh. Donec sodales sagittis magna. Sed consequat, "
      + "leo eget bibendum sodales, augue velit cursus nunc, quis gravida magna mi a libero"
      + ". Fusce vulputate eleifend sapien. Vestibulum purus quam, scelerisque ut, mollis s"
      + "ed, nonummy id, metus. Nullam accumsan lorem in dui. Cras ultricies mi";
  private static final String TITLE_TEST_NAME = "Title Test Name";
  private final TitlesPostBodyValidator validator = new TitlesPostBodyValidator(
    new TitleCommonRequestAttributesValidator(),
    new CustomLabelsProperties(50, 100)
  );

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPostBody() {
    TitlePostRequest postRequest = null;
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyBody() {
    TitlePostRequest postRequest = new TitlePostRequest();
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPostData() {
    TitlePostRequest postRequest = new TitlePostRequest()
      .withData(null);
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostData() {
    TitlePostRequest postRequest = new TitlePostRequest()
      .withData(new TitlePostData());
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitleDescriptionIsTooLong() {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(new TitlePostDataAttributes()
          .withName(TITLE_TEST_NAME)
          .withDescription(TEXT_LONGER_THAN_1500_CHARACTERS)));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitleNameIsTooLong() {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(new TitlePostDataAttributes()
          .withName(TEXT_LONGER_THAN_400_CHARACTERS)));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitleNameIsNull() {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(new TitlePostDataAttributes()
          .withName(null)));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitleNameIsEmpty() {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(new TitlePostDataAttributes()
          .withName("")));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitlePublisherNameIsTooLong() {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(new TitlePostDataAttributes()
          .withName(TITLE_TEST_NAME)
          .withPublisherName(TEXT_LONGER_THAN_250_CHARACTERS)));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitleEditionIsTooLong() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublisherName(
        TEXT_LONGER_THAN_250_CHARACTERS));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfTitleIdentifierIdTooLong() {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    List<Identifier> titleIdentifiers = new ArrayList<>();
    titleIdentifiers.add(new Identifier().withId("1234567-1234567-1234567"));

    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(new TitlePostDataAttributes()
          .withName(TITLE_TEST_NAME)
          .withPublisherName("Test publisher name").withIdentifiers(titleIdentifiers)));

    validator.validate(titlePostRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionIfUserDefinedFieldIsTooLong() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withUserDefinedField1(StringUtils.repeat("*", 101))
      .withPublicationType(PublicationType.BOOK));

    validator.validate(titlePostRequest);
  }

  @Test
  public void shouldNotThrowExceptionIfTitleDescriptionIsEmpty() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublicationType(PublicationType.BOOK)
      .withDescription(""));

    validator.validate(titlePostRequest);
  }

  @Test
  public void shouldNotThrowExceptionIfTitleDescriptionIsNull() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublicationType(PublicationType.BOOK)
      .withDescription(null));

    validator.validate(titlePostRequest);
  }

  @Test
  public void shouldNotThrowExceptionIfTitleEditionIsEmpty() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublicationType(PublicationType.BOOK)
      .withEdition(""));

    validator.validate(titlePostRequest);
  }

  @Test
  public void shouldNotThrowExceptionIfTitleEditionIsNull() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublicationType(PublicationType.BOOK)
      .withEdition(null));

    validator.validate(titlePostRequest);
  }

  @Test
  public void shouldNotThrowExceptionIfTitlePublisherNameIsNull() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublicationType(PublicationType.BOOK)
      .withPublisherName(null));

    validator.validate(titlePostRequest);
  }

  @Test
  public void shouldNotThrowExceptionIfTitlePublisherNameIsEmpty() {
    TitlePostRequest titlePostRequest = createRequest(new TitlePostDataAttributes()
      .withName(TITLE_TEST_NAME)
      .withPublicationType(PublicationType.BOOK)
      .withPublisherName(""));

    validator.validate(titlePostRequest);
  }

  private List<TitlePostIncluded> getTitlePostIncluded() {
    List<TitlePostIncluded> titleIncluded = new ArrayList<>();
    titleIncluded.add(new TitlePostIncluded()
      .withType("resource")
      .withAttributes(new TitlePostIncludedPackageId()
        .withPackageId("123456-123456")));
    return titleIncluded;
  }

  private TitlePostRequest createRequest(TitlePostDataAttributes attributes) {
    TitlePostRequest titlePostRequest = new TitlePostRequest();
    titlePostRequest
      .withData(new TitlePostData()
        .withAttributes(attributes))
      .withIncluded(getTitlePostIncluded());
    return titlePostRequest;
  }
}
