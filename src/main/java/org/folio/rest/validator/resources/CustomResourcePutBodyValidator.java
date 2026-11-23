package org.folio.rest.validator.resources;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.RequiredArgsConstructor;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomResourcePutBodyValidator {

  private static final int URL_MAX_LENGTH = 600;

  private final ResourceCommonAttributesValidator commonAttributesValidator;

  public void validate(ResourcePutRequest request) {
    var attributes = request.getData().getAttributes();

    // A custom resource is always selected, so its attributes are always validated
    String url = attributes.getUrl();
    if (isNotBlank(url)) {
      ValidatorUtil.checkMaxLength("url", url, URL_MAX_LENGTH);
      ValidatorUtil.checkUrlFormat("url", url);
    }
    commonAttributesValidator.validate(attributes);
  }
}
