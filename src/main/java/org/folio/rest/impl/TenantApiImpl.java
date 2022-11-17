package org.folio.rest.impl;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.liquibase.LiquibaseUtil;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;

public class TenantApiImpl extends TenantAPI {

  private static final Logger logger = LogManager.getLogger(TenantApiImpl.class);

  private static final String TEST_DATA_SQL = "templates/db_scripts/test-data.sql";
  private static final String TENANT_PLACEHOLDER = "${myuniversity}";
  private static final String MODULE_PLACEHOLDER = "${mymodule}";
  private static final String TEST_MODE = "test.mode";

  @Override
  Future<Integer> loadData(TenantAttributes attributes, String tenantId,
                           Map<String, String> headers, Context context) {
    return super.loadData(attributes, tenantId, headers, context)
      .compose(num -> {
        Vertx vertx = context.owner();
        logger.info("************ Running schema updates ************");
        LiquibaseUtil.initializeSchemaForTenant(vertx, tenantId);
        logger.info("Schema updated for tenant: {}", tenantId);
        return setupTestData(vertx, tenantId).map(num);
      });
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

      String sqlScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
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
