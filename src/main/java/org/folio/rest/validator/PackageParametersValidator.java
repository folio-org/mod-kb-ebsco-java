package org.folio.rest.validator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.validation.ValidationException;
import org.folio.rest.model.Sort;

public class PackageParametersValidator {

  private static final List<String> FILTER_SELECTED_VALUES = Arrays.asList("true", "false", "ebsco");
  private static final List<String> FILTER_TYPE_VALUES = Arrays.asList("all", "aggregatedfulltext", "abstractandindex", "ebook", "ejournal", "print", "unknown", "onlinereference");

  public void validate(String filterCustom, String filterSelected, String filterType,
    String sort) {

    if (!Sort.contains(sort.toUpperCase())){
      throw new ValidationException("Invalid Query Parameter for sort");
    }
    if(Objects.nonNull(filterType) &&  !FILTER_TYPE_VALUES.contains(filterType)){
      throw new ValidationException("Invalid Query Parameter for filter[:type]");
    }
    if (Objects.nonNull(filterSelected) && !FILTER_SELECTED_VALUES.contains(filterSelected)){
      throw new ValidationException("Invalid Query Parameter for filter[:selected]");
    }
    if(Objects.nonNull(filterCustom) && !Boolean.parseBoolean(filterCustom)){
      throw new ValidationException("Invalid Query Parameter for filter[:custom]");
    }
  }
}
