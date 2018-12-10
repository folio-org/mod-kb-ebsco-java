package org.folio.config;

import io.vertx.core.shareddata.Shareable;
import lombok.Builder;
import lombok.Value;

/**
 * Contains the RM API connection details from mod-configuration.
 */
@Value
@Builder(toBuilder = true)
public final class RMAPIConfiguration implements Shareable {
  private final String customerId;
  private final String apiKey;
  private final String url;
  private final Boolean configValid;

  public String getCustomerId() {
    return customerId;
  }

  public String getAPIKey() {
    return apiKey;
  }

  public String getUrl() {
    return url;
  }

  public Boolean getConfigValid() {
    return configValid;
  }

  @Override
  public String toString() {
    return "RMAPIConfiguration [customerId=" + customerId
      + ", apiKey=" + apiKey + ", url=" + url + ']';
  }
}
