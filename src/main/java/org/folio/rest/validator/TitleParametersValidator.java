package org.folio.rest.validator;

import org.folio.rest.model.Sort;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TitleParametersValidator {

  private static final List<String> FILTER_SELECTED_VALUES = Arrays.asList("true", "false", "ebsco");
  private static final List<String> FILTER_TYPE_VALUES = Arrays.asList("audiobook", "book", "bookseries", "database", "journal", "newsletter", "newspaper", "proceedings", "report", "streamingaudio", "streamingvideo", "thesisdissertation", "website", "unspecified");

  /**
   * @throws ValidationException if validation fails
   */
  public void validate(String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject,
                       String filterPublisher, String sort) {
    List<String> searchParameters = Arrays.asList(filterName, filterIsxn, filterSubject, filterPublisher);
    long nonNullFilters = searchParameters.stream()
      .filter(Objects::nonNull)
      .count();
    if(nonNullFilters > 1){
      throw new ValidationException("Conflicting filter parameters");
    }
    if(nonNullFilters < 1){
      throw new ValidationException("All of filter[name], filter[isxn], filter[subject] and filter[publisher] cannot be missing.");
    }
    if(searchParameters.stream()
      .anyMatch(""::equals)){
      throw new ValidationException("Value of required parameter filter[name], filter[isxn], filter[subject] or filter[publisher] is missing.");
    }
    if (!Sort.contains(sort.toUpperCase())){
      throw new ValidationException("Invalid sort parameter");
    }
    if (Objects.nonNull(filterSelected) && !FILTER_SELECTED_VALUES.contains(filterSelected)){
      throw new ValidationException("Invalid Query Parameter for filter[selected]");
    }
    if (Objects.nonNull(filterType) && !FILTER_TYPE_VALUES.contains(filterType)){
      throw new ValidationException("Invalid Query Parameter for filter[type]");
    }
  }
}

