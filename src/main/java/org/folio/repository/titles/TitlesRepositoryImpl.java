package org.folio.repository.titles;

import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.repository.DbUtil.createInsertOrUpdateParameters;
import static org.folio.repository.titles.TitlesTableConstants.DELETE_TITLE_STATEMENT;
import static org.folio.repository.titles.TitlesTableConstants.INSERT_OR_UPDATE_TITLE_STATEMENT;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Title;
import org.folio.rest.persist.PostgresClient;

@Component
public class TitlesRepositoryImpl implements TitlesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TitlesRepositoryImpl.class);

  private Vertx vertx;

  @Autowired
  public TitlesRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveTitle(Title title, String tenantId) {
    JsonArray parameters = createInsertOrUpdateParameters(String.valueOf(title.getTitleId()), title.getTitleName());

    final String query = String.format(INSERT_OR_UPDATE_TITLE_STATEMENT, getTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteTitle(String titleId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(titleId));

    final String query = String.format(DELETE_TITLE_STATEMENT, getTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  private String getTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + TITLES_TABLE_NAME;
  }
}
