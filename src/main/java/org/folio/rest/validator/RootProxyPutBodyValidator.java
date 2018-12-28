package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.springframework.stereotype.Component;

@Component
public class RootProxyPutBodyValidator {
    private static final String INVALID_PROXY_ID = "Invalid proxy id";
    private static final String PROXY_NOT_NULL = "proxyTypeId cannot be null";

    /**
     * @throws InputValidationException
     *           if PUT validation fails
     *
     *           We are checking only for null case because RM API handles every other case
     *           if the proxy is invalid and gives a 400 except for null in case of which it gives
     *           a 500.
     */
    public void validate(RootProxyPutRequest putRequest) {
      if (putRequest != null
          && putRequest.getData() != null
          && putRequest.getData().getAttributes() != null
          && putRequest.getData().getAttributes().getProxyTypeId() == null) {
        throw new InputValidationException(INVALID_PROXY_ID, PROXY_NOT_NULL);
      }
    }
  }
