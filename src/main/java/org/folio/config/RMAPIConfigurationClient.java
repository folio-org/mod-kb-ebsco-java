package org.folio.config;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.utils.TenantTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Retrieves the RM API connection details from mod-configuration.
 */
public class RMAPIConfigurationClient {

  private static final Logger LOG = LoggerFactory.getLogger(RMAPIConfigurationClient.class);

  private static final String EBSCO_URL_CODE = "kb.ebsco.url";
  private static final String EBSCO_API_KEY_CODE = "kb.ebsco.apiKey";
  private static final String EBSCO_CUSTOMER_ID_CODE = "kb.ebsco.customerId";

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
      ConfigurationsClient configurationsClient = configurationClientProvider.createClient(url.getHost(), url.getPort(), tenantId, apiToken);
      configurationsClient.getEntries("module=EKB", 0, Integer.MAX_VALUE, null, null, response ->
        response.bodyHandler(body -> {
          if (!Response.isSuccess(response.statusCode())) {
            LOG.error("Cannot get configuration data: error code - {} response body - {}", response.statusCode(), body);
            future.completeExceptionally(new IllegalStateException(body.toString()));
          }
          JsonObject entries = body.toJsonObject();
          future.complete(mapResults(entries.getJsonArray("configs")));
        }));
    } catch (IOException e) {
      LOG.error("Cannot get configuration data: " + e.getMessage(), e);
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
