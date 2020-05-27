package org.folio.rest.validator;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;

@Component
public class CustomLabelsPutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_LABEL_IDS_MESSAGE = "Each label in body must contain unique id";

  private static final String CUSTOM_LABEL_ID_PARAM = "Custom Label id";
  private static final String CUSTOM_LABEL_NAME_PARAM = "Custom Label Name";
  private static final String FULL_TEXT_FINDER_PARAM = "Full Text Finder";
  private static final String PUBLICATION_FINDER_PARAM = "Publication Finder";

  private static final int CUSTOM_LABEL_NAME_MAX_LENGTH = 50;

  public void validate(@NotNull CustomLabelsPutRequest request) {
    checkEachLabelIsValid(request);
    checkLabelsHaveUniqueIds(request);
  }

  private void checkEachLabelIsValid(@NotNull CustomLabelsPutRequest request) {
    request.getData().forEach(this::validateCollectionItem);
  }

  private void validateCollectionItem(@NotNull CustomLabel customLabel) {
    CustomLabelDataAttributes attributes = customLabel.getAttributes();
    ValidatorUtil.checkInRange(1, 5, attributes.getId(), CUSTOM_LABEL_ID_PARAM);
    ValidatorUtil.checkMaxLength(CUSTOM_LABEL_NAME_PARAM, attributes.getDisplayLabel(), CUSTOM_LABEL_NAME_MAX_LENGTH);
    ValidatorUtil.checkIsNotNull(FULL_TEXT_FINDER_PARAM, attributes.getDisplayOnFullTextFinder());
    ValidatorUtil.checkIsNotNull(PUBLICATION_FINDER_PARAM, attributes.getDisplayOnPublicationFinder());
  }

  private void checkLabelsHaveUniqueIds(@NotNull CustomLabelsPutRequest request) {
    if (!request.getData().isEmpty() && getUniqueIdsCount(request) != request.getData().size()) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_LABEL_IDS_MESSAGE);
    }
  }

  private long getUniqueIdsCount(@NotNull CustomLabelsPutRequest request) {
    return request.getData().stream()
      .map(CustomLabel::getAttributes)
      .filter(Objects::nonNull)
      .map(CustomLabelDataAttributes::getId)
      .distinct()
      .count();
  }
}
