package org.folio.rest.validator;

import static java.util.Objects.nonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.validation.ValidationException;

import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.Sort;

@Component
public class TitleParametersValidator {

  private static final List<String> FILTER_SELECTED_VALUES = Arrays.asList("true", "false", "ebsco");
  private static final List<String> FILTER_TYPE_VALUES = Arrays.asList("audiobook", "book", "bookseries", "database",
    "journal", "newsletter", "newspaper", "proceedings", "report", "streamingaudio", "streamingvideo",
    "thesisdissertation", "website", "unspecified");


  /**
   * @throws ValidationException if validation fails
   */
  public void validate(FilterQuery filterQuery, String sort) {
    Boolean allowNullFilters = false;
    validateFilter(filterQuery, allowNullFilters);
    validateSort(sort);
  }

  public void validate(FilterQuery filterQuery, String sort, Boolean allowNullFilters) {
    validateFilter(filterQuery, allowNullFilters);
    validateSort(sort);
  }

  private void validateFilter(FilterQuery fq, Boolean allowNullFilters) {
    List<String> searchParameters = Arrays.asList(fq.getName(), fq.getIsxn(), fq.getSubject(),
      fq.getPublisher());

    long nonNullFilters = searchParameters.stream()
      .filter(Objects::nonNull)
      .count();
    if(nonNullFilters > 1){
      throw new ValidationException("Conflicting filter parameters");
    }
    if((nonNullFilters < 1)&&(!allowNullFilters)){
      throw new ValidationException("All of filter[name], filter[isxn], filter[subject] and filter[publisher] cannot be missing.");
    }
    if(searchParameters.stream()
      .anyMatch(""::equals)){
      throw new ValidationException("Value of required parameter filter[name], filter[isxn], filter[subject] or filter[publisher] is missing.");
    }
    if (nonNull(fq.getSelected()) && !FILTER_SELECTED_VALUES.contains(fq.getSelected())){
      throw new ValidationException("Invalid Query Parameter for filter[selected]");
    }
    if (nonNull(fq.getType()) && !FILTER_TYPE_VALUES.contains(fq.getType())){
      throw new ValidationException("Invalid Query Parameter for filter[type]");
    }
  }

  private void validateSort(String sort) {
    if (!Sort.contains(sort.toUpperCase())){
      throw new ValidationException("Invalid sort parameter");
    }
  }

}

