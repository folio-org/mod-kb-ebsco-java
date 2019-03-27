package org.folio.rest.validator;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;

import static org.folio.rest.util.RestConstants.PACKAGE_RECTYPE;
import static org.folio.rest.util.RestConstants.PROVIDER_RECTYPE;
import static org.folio.rest.util.RestConstants.RESOURCE_RECTYPE;
import static org.folio.rest.util.RestConstants.TITLE_RECTYPE;

import java.util.Arrays;
import java.util.List;

import javax.validation.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class RectypeParameterValidator {

  private static final String PARAM_NAME = "filter[rectype]";
  private static final List<String> FILTER_RECTYPE_VALUES = Arrays.asList(PROVIDER_RECTYPE, PACKAGE_RECTYPE,
      TITLE_RECTYPE,RESOURCE_RECTYPE);

  
  public void validate(String rectype) {
    if (StringUtils.isBlank(rectype)) {
      throw new ValidationException(format("Parameter '%s' cannot be empty", PARAM_NAME));
    }
    if (!FILTER_RECTYPE_VALUES.contains(rectype)) {
      throw new ValidationException(format("Invalid '%s' parameter value: %s. Possible values: %s",
        PARAM_NAME, rectype, join(FILTER_RECTYPE_VALUES, ", ")));
    }
  }
}
