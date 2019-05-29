package org.folio.rest.impl;

import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.STUB_TOKEN;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;

public class TestSetUpHelper {
  private static final String HTTP_PORT = "http.port";

  public static int port;
  public static String host;
  public static Vertx vertx;
  public static boolean started;

  public static void startVertxAndPostgres() throws IOException {
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();
    host = "http://127.0.0.1";

    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), getDeploymentOptions(), event -> future.complete(null));
    future.join();

    PostgresClient.getInstance(vertx)
      .startEmbeddedPostgres();
    postTenant();
    started = true;
  }

  public static void stopVertxAndPostgres() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.close(res -> {
      PostgresClient.stopEmbeddedPostgres();
      future.complete(null);
    });
    future.join();
    started = false;
  }

  public static boolean isStarted() {
    return started;
  }

  public static int getPort() {
    return port;
  }

  public static String getHost() {
    return host;
  }

  public static Vertx getVertx() {
    return vertx;
  }

  private static void postTenant() {
    TenantClient tenantClient = new TenantClient(host + ":" + port, STUB_TENANT, STUB_TOKEN);

    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), getDeploymentOptions(), res -> {
      try {
        tenantClient.postTenant(null, res2 -> future.complete(null));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    future.join();
  }

  private static DeploymentOptions getDeploymentOptions() {
    return new DeploymentOptions().setConfig(new JsonObject()
      .put(HTTP_PORT, port)
      .put("spring.configuration", "org.folio.spring.config.TestConfig")
    );
  }
}
