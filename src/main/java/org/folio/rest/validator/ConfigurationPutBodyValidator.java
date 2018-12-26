package org.folio.rest.validator;

import org.folio.rest.jaxrs.model.ConfigurationAttributes;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationPutBodyValidator {
  public void validate(ConfigurationPutRequest request) {
    ConfigurationAttributes attributes = request.getData().getAttributes();
    ValidatorUtil.checkIsNotNull("API key", attributes.getApiKey());
    ValidatorUtil.checkIsNotNull("Customer ID", attributes.getCustomerId());
    ValidatorUtil.checkIsNotNull("API endpoint", attributes.getRmapiBaseUrl());
  }
}
