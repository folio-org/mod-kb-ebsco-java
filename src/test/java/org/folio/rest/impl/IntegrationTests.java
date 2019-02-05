package org.folio.rest.impl;

import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.STUB_TOKEN;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Suite.SuiteClasses({
  EholdingsCacheImplTest.class,
  EholdingsConfigurationTest.class,
  EholdingsPackagesTest.class,
  EholdingsProvidersImplTest.class,
  EHoldingsProxyTypesImplTest.class,
  EholdingsResourcesImplTest.class,
  EHoldingsRootProxyImplTest.class,
  EholdingsStatusTest.class,
  EholdingsTitlesTest.class
})
@RunWith(Suite.class)
public class IntegrationTests {

  private static final String HTTP_PORT = "http.port";

  public static int port;
  public static String host;
  public static Vertx vertx;

  @BeforeClass
  public static void setUpClass() throws IOException {
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();
    host = "http://localhost";

    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));

    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions, event -> future.complete(null));
    future.join();

    PostgresClient.getInstance(vertx)
      .startEmbeddedPostgres();
    postTenant();
  }

  @AfterClass
  public static void tearDownClass() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.close(res -> {
      PostgresClient.stopEmbeddedPostgres();
      future.complete(null);
    });
    future.join();
  }

  private static void postTenant() {
    TenantClient tenantClient = new TenantClient(host + ":" + port, STUB_TENANT, STUB_TOKEN);

    final DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        tenantClient.postTenant(null, res2 -> future.complete(null));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    future.join();
  }
}
