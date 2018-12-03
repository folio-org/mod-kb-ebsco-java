package org.folio.config.exception;

import java.util.List;

import org.folio.config.model.ConfigurationError;

public class RMAPIConfigurationInvalidException extends RuntimeException {

  private static final long serialVersionUID = 5325789760372474463L;

  private final List<ConfigurationError> errors;

  public RMAPIConfigurationInvalidException(List<ConfigurationError> errors) {
    this.errors = errors;
  }

  public List<ConfigurationError> getErrors() {
    return errors;
  }
}
