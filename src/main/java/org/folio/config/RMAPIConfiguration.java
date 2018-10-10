package org.folio.config;

/**
 * Contains the RM API connection details from mod-configuration.
 */
public final class RMAPIConfiguration {
  private String customerId;
  private String apiKey;
  private String url;

  public String getCustomerId() {
    return customerId;
  }

  public String getAPIKey() {
    return apiKey;
  }

  public String getUrl() {
    return url;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return "RMAPIConfiguration [customerId=" + customerId
      + ", apiKey=" + apiKey + ", url=" + url + ']';
  }
}
