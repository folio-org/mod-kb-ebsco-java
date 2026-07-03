package org.folio.rest.validator.packages;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
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

  private static final String INVALID_BEGIN_COVERAGE = "Begin Coverage has an invalid date.";

  public void validate(PackageCustomAttributes customAttributes) {
    validateCustomDescription(customAttributes.customDescription);
    validateCustomDisplayName(customAttributes.customDisplayName);
    validateCustomAltNames(customAttributes.customAltNames);
    validateUrl(customAttributes.url);
    validateCustomCoverage(customAttributes.customCoverage);
  }

  private void validateCustomDescription(String description) {
    ValidatorUtil.checkMaxLength("customDescription", description, MAX_DESCRIPTION_LENGTH);
  }

  private void validateCustomDisplayName(String displayName) {
    ValidatorUtil.checkMaxLength("customDisplayName", displayName, MAX_DISPLAY_NAME_LENGTH);
    ValidatorUtil.checkNoHtml("customDisplayName", displayName);
  }

  private void validateCustomAltNames(List<PackageAltName> altNames) {
    if (altNames == null) {
      return;
    }
    ValidatorUtil.checkCollectionSize("customAltNames", altNames, MAX_ALT_NAMES_COUNT);
    for (var altName : altNames) {
      var name = altName.getAltName();
      ValidatorUtil.checkMaxLength("customAltNames.altName", name, MAX_ALT_NAME_LENGTH);
      ValidatorUtil.checkNoHtml("customAltNames", name);
    }
  }

  private void validateUrl(String url) {
    if (url == null) {
      return;
    }
    ValidatorUtil.checkMaxLength("url", url, MAX_URL_LENGTH);
    ValidatorUtil.checkUrlFormat("url", url);
  }

  private void validateCustomCoverage(Coverage coverage) {
    if (coverage == null) {
      return;
    }
    var beginCoverage = coverage.getBeginCoverage();
    var endCoverage = coverage.getEndCoverage();
    ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);
    ValidatorUtil.check("coverage", coverage,
      c -> StringUtils.isEmpty(c.getBeginCoverage()) && !StringUtils.isEmpty(c.getEndCoverage()),
      INVALID_BEGIN_COVERAGE);
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
