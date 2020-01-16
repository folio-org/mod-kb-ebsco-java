package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CustomLabelsPutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data";
  private static final String INVALID_REQUEST_LABEL_IDS = "Each label in body must contain unique id";
  private static final String INVALID_REQUEST_LABEL_ATTRIBUTES = "Each label in body must contain attributes";

  private static final String CUSTOM_LABEL_ID_PARAM = "Custom Label id";
  private static final String CUSTOM_LABEL_NAME_PARAM = "Custom Label Name";
  private static final String FULL_TEXT_FINDER_PARAM = "Full Text Finder";
  private static final String PUBLICATION_FINDER_PARAM = "Publication Finder";

  private static final int CUSTOM_LABEL_NAME_MAX_LENGTH = 50;

  public void validate(CustomLabelPutRequest request) {
    checkRequestHasData(request);
    checkEachLabelIsValid(request);
    checkLabelsHaveUniqueIds(request);
  }

  private void checkEachLabelIsValid(CustomLabelPutRequest request) {
    request.getData().forEach(this::validateCollectionItem);
  }

  private void checkRequestHasData(CustomLabelPutRequest request) {
    if (request == null || request.getData() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
  }

  private void validateCollectionItem(CustomLabelCollectionItem customLabelsCollectionItem) {
    if (customLabelsCollectionItem == null || customLabelsCollectionItem.getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_LABEL_ATTRIBUTES);
    }
    final CustomLabelDataAttributes attributes = customLabelsCollectionItem.getAttributes();
    ValidatorUtil.checkInRange(1, 5, attributes.getId(), CUSTOM_LABEL_ID_PARAM);
    ValidatorUtil.checkMaxLength(CUSTOM_LABEL_NAME_PARAM, attributes.getDisplayLabel(), CUSTOM_LABEL_NAME_MAX_LENGTH);
    ValidatorUtil.checkIsNotNull(FULL_TEXT_FINDER_PARAM, attributes.getDisplayOnFullTextFinder());
    ValidatorUtil.checkIsNotNull(PUBLICATION_FINDER_PARAM, attributes.getDisplayOnPublicationFinder());
  }

  private void checkLabelsHaveUniqueIds(CustomLabelPutRequest request) {
    if (!request.getData().isEmpty() && getUniqueIdsCount(request) != request.getData().size()) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_LABEL_IDS);
    }
  }

  private long getUniqueIdsCount(CustomLabelPutRequest request) {
    return request.getData().stream()
      .map(CustomLabelCollectionItem::getAttributes)
      .filter(Objects::nonNull)
      .map(CustomLabelDataAttributes::getId)
      .distinct()
      .count();
  }
}
