package org.folio.util;

import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.STUB_TOKEN;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;

public class TestSetUpHelper {

  private static final String HTTP_PORT = "http.port";
  private static final String MODULE_TO_VERSION = "mod-1.0.0";
  private static final int TENANT_OP_WAITING_TIME = 60000;

  private static int port;
  private static String host;
  private static Vertx vertx;

  public static void startVertxAndPostgres(Map<String, String> configProperties) {
    port = NetworkUtils.nextFreePort();
    host = "http://127.0.0.1";
    vertx = Vertx.vertx();
    startPostgresContainer();

    var future = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), getDeploymentOptions(configProperties))
      .onComplete(event -> {
        var tenantClient = new TenantClient(getModuleUrl(), STUB_TENANT, STUB_TOKEN, vertx.createHttpClient());
        try {
          var tenantAttributes = new TenantAttributes().withModuleTo(MODULE_TO_VERSION);
          tenantClient.postTenant(tenantAttributes, res1 -> {
            if (res1.succeeded()) {
              checkTenantOpStatus(res1, tenantClient, future);
            } else {
              future.completeExceptionally(new IllegalStateException("Failed to create tenant job"));
            }
          });
        } catch (Exception e) {
          future.completeExceptionally(e);
        }
      });
    future.join();
  }

  public static void stopVertxAndPostgres() {
    var future = new CompletableFuture<>();
    vertx.close().onComplete(res -> future.complete(null));
    future.join();
  }

  public static Vertx getVertx() {
    return vertx;
  }

  public static String getModuleUrl() {
    return host + ":" + port;
  }

  private static void checkTenantOpStatus(AsyncResult<HttpResponse<Buffer>> result, TenantClient tenantClient,
                                          CompletableFuture<Object> future) {
    var jobId = result.result().bodyAsJson(TenantJob.class).getId();
    tenantClient.getTenantByOperationId(jobId, TENANT_OP_WAITING_TIME, res2 -> {
      if (res2.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(new IllegalStateException("Failed to get tenant"));
      }
    });
  }

  private static void startPostgresContainer() {
    try {
      PostgresClient.setPostgresTester(new PostgresTesterContainer());
      PostgresClient.getInstance(vertx).startPostgresTester();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize postgres client", e);
    }
  }

  private static DeploymentOptions getDeploymentOptions(Map<String, String> configProperties) {
    JsonObject config = new JsonObject()
      .put(HTTP_PORT, port);
    configProperties.forEach(config::put);
    return new DeploymentOptions().setConfig(config
    );
  }
}
