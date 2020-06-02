package org.folio.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.folio.liquibase.LiquibaseUtil;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.spring.SpringContextUtil;

public class TenantApiImpl extends TenantAPI {

  private static final Logger logger = LoggerFactory.getLogger(TenantApiImpl.class);

  private static final String TEST_DATA_SQL = "templates/db_scripts/test-data.sql";
  private static final String TENANT_PLACEHOLDER = "${myuniversity}";
  private static final String MODULE_PLACEHOLDER = "${mymodule}";
  private static final String TEST_MODE = "test.mode";


  public TenantApiImpl() {
    REMOVE ME!!
    super();
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postTenant(TenantAttributes entity, Map<String, String> headers, Handler<AsyncResult<Response>> handlers,
                         Context context) {

    Promise<Response> postTenant = Promise.promise();
    super.postTenant(entity, headers, postTenant, context);

    String tenantId = TenantTool.tenantId(headers);
    Vertx vertx = context.owner();

    postTenant.future()
      .compose(executeSchemaScripts(vertx, tenantId)::map)
      .compose(setupTestData(vertx, tenantId)::map)
      .setHandler(handlers);
  }

  private Future<Object> executeSchemaScripts(Vertx vertx, String tenantId) {
    Promise<Object> promise = Promise.promise();

    vertx.executeBlocking(p -> {
      logger.info("************ Running schema updates ************");

      LiquibaseUtil.initializeSchemaForTenant(vertx, tenantId);

      logger.info("Schema updated for tenant: {}", tenantId);
      p.complete();
    }, promise);

    return promise.future();
  }

  private Future<List<String>> setupTestData(Vertx vertx, String tenantId) {
    try {

      if (!Boolean.TRUE.equals(Boolean.valueOf(System.getenv(TEST_MODE)))) {
        logger.info("Test data will not be initialized.");
        return Future.succeededFuture();
      }

      InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TEST_DATA_SQL);

      if (inputStream == null) {
        logger.info("Test data will not be initialized: no resources found: {}", TEST_DATA_SQL);
        return Future.succeededFuture();
      }

      String sqlScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
      if (StringUtils.isBlank(sqlScript)) {
        return Future.succeededFuture();
      }

      String moduleName = PostgresClient.getModuleName();

      sqlScript = sqlScript.replace(TENANT_PLACEHOLDER, tenantId).replace(MODULE_PLACEHOLDER, moduleName);

      Promise<List<String>> promise = Promise.promise();
      PostgresClient.getInstance(vertx).runSQLFile(sqlScript, false, promise);

      logger.info("Received flag to initialize test data. Check the server log for details.");
      logger.info("************ Creating test data ************");

      return promise.future();
    } catch (IOException e) {
      return Future.failedFuture(e);
    }
  }
}
