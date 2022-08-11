package org.folio.rest.validator;

import java.util.Collections;
import java.util.List;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePostData;
import org.folio.rest.jaxrs.model.ResourcePostDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.junit.Test;

public class ResourcePostValidatorTest {

  private static final String PACKAGE_ID = "123-456";
  private static final String TITLE_ID = "789";
  private static final String TITLE_NAME = "title name";
  private static final int OTHER_TITLE_ID = 555;

  private final ResourcePostValidator validator = new ResourcePostValidator();

  @Test
  public void shouldValidateRequest() {
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, "http://example.com"));
  }

  @Test
  public void shouldValidateWhenUrlIsNullOrEmpty() {
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, null));
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, ""));
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenTitleIdIsNotPresent() {
    validator.validate(createRequest(PACKAGE_ID, null, "http://example.com"));
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPackageIdIsNotPresent() {
    validator.validate(createRequest(null, TITLE_ID, "http://example.com"));
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenUrlIsInvalid() {
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, "hdttp://example.com"));
  }

  @Test
  public void shouldValidateTitleAndPackage() {
    validator.validateRelatedObjects(
      createPackage().build(),
      createTitle().build(),
      createTitles().build());
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenPackageIsNotCustom() {
    PackageByIdData packageData = createPackage()
      .isCustom(false)
      .build();
    validator.validateRelatedObjects(
      packageData,
      createTitle().build(),
      createTitles().build());
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenTitleIsAlreadyAddedToPackage() {
    Title title = createTitle().build();
    Titles titles = createTitles().titleList(Collections.singletonList(title)).build();
    PackageByIdData packageData = createPackage().build();
    validator.validateRelatedObjects(
      packageData,
      title,
      titles);
  }

  private Titles.TitlesBuilder createTitles() {
    List<Title> titles = Collections.singletonList(createTitle().titleId(OTHER_TITLE_ID).build());
    return Titles.builder().titleList(titles);
  }

  private ResourcePostRequest createRequest(String packageId, String titleId, String url) {
    ResourcePostRequest request = new ResourcePostRequest();
    request.setData(new ResourcePostData());
    request.getData().setAttributes(new ResourcePostDataAttributes());
    request.getData().setType(ResourcePostData.Type.RESOURCES);
    request.getData().getAttributes().setPackageId(packageId);
    request.getData().getAttributes().setTitleId(titleId);
    request.getData().getAttributes().setUrl(url);
    return request;
  }

  private PackageByIdData.PackageByIdDataBuilder createPackage() {
    return PackageByIdData.byIdBuilder()
      .packageName("package")
      .vendorId(123)
      .packageId(456)
      .isCustom(true);
  }

  private Title.TitleBuilder createTitle() {
    return Title.builder()
      .titleId(Integer.valueOf(TITLE_ID))
      .titleName(TITLE_NAME);
  }
}
