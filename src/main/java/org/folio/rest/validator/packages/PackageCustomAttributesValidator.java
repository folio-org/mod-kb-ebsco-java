package org.folio.rest.validator.packages;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageAltName;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

@Component
public class PackageCustomAttributesValidator {

  private static final int MAX_DESCRIPTION_LENGTH = 2000;
  private static final int MAX_DISPLAY_NAME_LENGTH = 300;
  private static final int MAX_ALT_NAME_LENGTH = 300;
  private static final int MAX_ALT_NAMES_COUNT = 10;
  private static final int MAX_URL_LENGTH = 500;

  private static final String DESCRIPTION_TOO_LONG = "2000 character limit has been exceeded.";
  private static final String DISPLAY_NAME_TOO_LONG = "300 character limit has been exceeded.";
  private static final String DISPLAY_NAME_CONTAINS_HTML =
    "Invalid characters. Custom alternate name field does not accept HTML.";
  private static final String ALT_NAMES_LIMIT_EXCEEDED =
    "Custom alternate names maximum limit of 10 has been exceeded.";
  private static final String ALT_NAME_TOO_LONG = "300 character limit has been exceeded.";
  private static final String ALT_NAME_CONTAINS_HTML =
    "Invalid characters. Custom alternate name field does not accept HTML.";
  private static final String URL_TOO_LONG = "500 character limit has been exceeded.";
  private static final String INVALID_BEGIN_COVERAGE = "Begin Coverage has an invalid date.";

  public void validate(PackageCustomAttributes customAttributes) {
    validateCustomDescription(customAttributes.customDescription);
    validateCustomDisplayName(customAttributes.customDisplayName);
    validateCustomAltNames(customAttributes.customAltNames);
    validateUrl(customAttributes.url);
    validateCustomCoverage(customAttributes.customCoverage);
  }

  private void validateCustomDescription(String description) {
    if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
      throw new InputValidationException(DESCRIPTION_TOO_LONG, "");
    }
  }

  private void validateCustomDisplayName(String displayName) {
    if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
      throw new InputValidationException(DISPLAY_NAME_TOO_LONG, "");
    }
    ValidatorUtil.checkNoHtml("customDisplayName", displayName, DISPLAY_NAME_CONTAINS_HTML);
  }

  private void validateCustomAltNames(List<PackageAltName> altNames) {
    if (altNames == null) {
      return;
    }
    if (altNames.size() > MAX_ALT_NAMES_COUNT) {
      throw new InputValidationException(ALT_NAMES_LIMIT_EXCEEDED, "");
    }
    for (PackageAltName altName : altNames) {
      String name = altName.getAltName();
      if (name != null && name.length() > MAX_ALT_NAME_LENGTH) {
        throw new InputValidationException(ALT_NAME_TOO_LONG, "");
      }
      ValidatorUtil.checkNoHtml("customAltNames", name, ALT_NAME_CONTAINS_HTML);
    }
  }

  private void validateUrl(String url) {
    if (url == null) {
      return;
    }
    if (url.length() > MAX_URL_LENGTH) {
      throw new InputValidationException(URL_TOO_LONG, "");
    }
    ValidatorUtil.checkUrlFormat("url", url);
  }

  private void validateCustomCoverage(Coverage coverage) {
    if (coverage == null) {
      return;
    }
    String beginCoverage = coverage.getBeginCoverage();
    String endCoverage = coverage.getEndCoverage();
    ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);
    if (StringUtils.isEmpty(beginCoverage) && !StringUtils.isEmpty(endCoverage)) {
      throw new InputValidationException(INVALID_BEGIN_COVERAGE, "");
    }
    if (StringUtils.isNotBlank(endCoverage)) {
      ValidatorUtil.checkDateValid("endCoverage", endCoverage);
      ValidatorUtil.checkDatesOrder(beginCoverage, endCoverage);
    }
  }

  public record PackageCustomAttributes(
    String customDescription,
    String customDisplayName,
    String url,
    List<PackageAltName> customAltNames,
    Coverage customCoverage
  ) { }
}
