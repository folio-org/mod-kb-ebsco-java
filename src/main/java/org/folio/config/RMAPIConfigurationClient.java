package org.folio.config;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.utils.TenantTool;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Retrieves the RM API connection details from mod-configuration.
 */
public class RMAPIConfigurationClient {

  private static final String EBSCO_URL_CODE = "kb.ebsco.url";
  private static final String EBSCO_API_KEY_CODE = "kb.ebsco.apiKey";
  private static final String EBSCO_CUSTOMER_ID_CODE = "kb.ebsco.customerId";
  private static final int QUERY_LIMIT = 100;

  private ConfigurationClientProvider configurationClientProvider;

  /**
   * @param configurationClientProvider object used to get http client for sending request to okapi
   */
  public RMAPIConfigurationClient(ConfigurationClientProvider configurationClientProvider) {
    this.configurationClientProvider = configurationClientProvider;
  }

  /**
   * Returns the RM API configuration for the tenant.
   *
   * @param apiToken token used to call okapi
   * @return The RMI API configuration for the tenant.
   */
  public CompletableFuture<RMAPIConfiguration> retrieveConfiguration(String apiToken, String tenant, String okapiURL) {
    final String tenantId = TenantTool.calculateTenantId(tenant);

    CompletableFuture<RMAPIConfiguration> future = new CompletableFuture<>();

    try {
      URL url = new URL(okapiURL);
      int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
      ConfigurationsClient configurationsClient = configurationClientProvider.createClient(url.getHost(), port, tenantId, apiToken);
      configurationsClient.getEntries("module=EKB", 0, QUERY_LIMIT, null, null, response ->
        response.bodyHandler(body -> {
          try {
            if (!Response.isSuccess(response.statusCode())) {
              throw new IllegalStateException("Cannot get configuration data: error code - " +
                response.statusCode() + " response body - " + body.toString());
            }
            JsonObject entries = body.toJsonObject();
            future.complete(mapResults(entries.getJsonArray("configs")));
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        }));
    } catch (IOException e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  /**
   * Simple mapper for the results of mod-configuration to RMAPIConfiguration.
   *
   * @param configs All the RM API related configurations returned by
   *                mod-configuration.
   */
  private RMAPIConfiguration mapResults(JsonArray configs) {
    RMAPIConfiguration config = new RMAPIConfiguration();
    configs.stream()
      .filter(JsonObject.class::isInstance)
      .map(JsonObject.class::cast)
      .forEach(entry -> {
        String code = entry.getString("code");
        String value = entry.getString("value");
        if (EBSCO_CUSTOMER_ID_CODE.equalsIgnoreCase(code)) {
          config.setCustomerId(value);
        } else if (EBSCO_API_KEY_CODE.equalsIgnoreCase(code)) {
          config.setApiKey(value);
        } else if (EBSCO_URL_CODE.equalsIgnoreCase(code)) {
          config.setUrl(value);
        }
      });

    if (config.getCustomerId() == null || config.getAPIKey() == null ||
      config.getUrl() == null) {
      throw new IllegalStateException("Configuration data is invalid");
    }

    return config;
  }
}
