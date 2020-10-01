package org.folio.rest.validator.uc;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rest.validator.ValidatorUtil;

@Component
public class UsageConsolidationPostBodyValidator {

  public static final String START_MONTH = "startMonth";
  public static final String PLATFORM_TYPE = "platformType";

  /**
   * @throws InputValidationException if validation of attributes fails
   */
  public void validate(UCSettingsPostRequest request) {

    validatePlatform(request.getData().getAttributes());
    validateMonth(request.getData().getAttributes());
  }

  private void validateMonth(UCSettingsDataAttributes attributes) {
    Month startMonth = attributes.getStartMonth();
    if(Objects.nonNull(startMonth)) {
      ValidatorUtil.checkEnumValue(Month.class, startMonth.value(), Arrays.toString(Month.values()), START_MONTH);
    } else {
      attributes.setStartMonth(Month.JAN);
    }
  }

  private void validatePlatform(UCSettingsDataAttributes attributes) {
    PlatformType platformType = attributes.getPlatformType();
    if(Objects.nonNull(platformType)) {
      ValidatorUtil.checkEnumValue(PlatformType.class, platformType.value(), Arrays.toString(PlatformType.values()), PLATFORM_TYPE);
    } else {
      attributes.setPlatformType(PlatformType.ALL);
    }
  }
}
