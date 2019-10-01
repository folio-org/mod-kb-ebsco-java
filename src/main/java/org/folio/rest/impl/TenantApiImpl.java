package org.folio.rest.impl;

import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.repository.holdings.status.RetryStatus;
import org.folio.repository.holdings.status.RetryStatusRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.spring.SpringContextUtil;

public class TenantApiImpl extends TenantAPI {

  private static final String TEST_DATA_SQL = "templates/db_scripts/test-data.sql";
  private static final String TENANT_PLACEHOLDER = "${myuniversity}";
  private static final String MODULE_PLACEHOLDER = "${mymodule}";
  private static final String TEST_MODE = "test.mode";
  private final Logger logger = LoggerFactory.getLogger(TenantApiImpl.class);

  @Autowired
  private HoldingsStatusRepository holdingsStatusRepository;
  @Autowired
  private RetryStatusRepository retryStatusRepository;

  public TenantApiImpl() {
    super();
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postTenant(TenantAttributes entity, Map<String, String> headers, Handler<AsyncResult<Response>> handlers,
                         Context context) {

    Future<Response> future = Future.future();
    super.postTenant(entity, headers, future, context);

    future.compose(response -> setupTestData(headers, context).map(response))
      .compose(response -> setLoadingStatus(headers).map(response))
      .setHandler(handlers);
  }



  private Future<Void> setLoadingStatus(Map<String, String> headers) {

    Future<Void> future = Future.future();
    String tenantId = TenantTool.tenantId(headers);
    setStatusNotStarted(tenantId)
      .thenCompose(o -> resetRetryStatus(tenantId))
      .thenAccept(future::complete)
      .exceptionally(e -> {
        future.fail(e.getCause());
        return null;
      });
    return future;
  }

  private CompletionStage<Void> resetRetryStatus(String tenantId) {
    return retryStatusRepository.delete(tenantId)
      .thenCompose(o -> retryStatusRepository.save(new RetryStatus(0, null), tenantId));
  }

  private CompletableFuture<Void> setStatusNotStarted(String tenantId) {
    return holdingsStatusRepository.save(getStatusNotStarted(), tenantId);
  }

  private Future<List<String>> setupTestData(Map<String, String> headers, Context context) {
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

      String tenantId = TenantTool.tenantId(headers);
      String moduleName = PostgresClient.getModuleName();

      sqlScript = sqlScript.replace(TENANT_PLACEHOLDER, tenantId).replace(MODULE_PLACEHOLDER, moduleName);

      Future<List<String>> future = Future.future();
      PostgresClient.getInstance(context.owner()).runSQLFile(sqlScript, false, future);

      logger.info("Received flag to initialize test data. Check the server log for details.");
      logger.info("************ Creating test data ************");

      return future;
    } catch (IOException e) {
      return Future.failedFuture(e);
    }
  }
}
