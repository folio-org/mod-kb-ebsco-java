package org.folio.http;

import org.folio.rest.client.ConfigurationsClient;
import org.springframework.stereotype.Component;

/**
 * Class used to create ConfigurationsClient, to allow mocking of client in unit tests
 */
@Component
public class ConfigurationClientProvider {
  public ConfigurationsClient createClient(String url, int port, String tenantId, String apiToken) {
    return new ConfigurationsClient(url, port, tenantId, apiToken);
  }
}
