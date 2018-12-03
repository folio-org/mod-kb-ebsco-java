package org.folio.config.model;

import java.io.Serializable;

public class ConfigurationError implements Serializable {

  private static final long serialVersionUID = -6757895174756465024L;

  private String message;

  public ConfigurationError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
