package org.folio.repository.titles;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.createInsertOrUpdateParameters;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.getTitlesTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.titles.TitlesTableConstants.COUNT_TITLES_BY_RESOURCE_TAGS;
import static org.folio.repository.titles.TitlesTableConstants.DELETE_TITLE_STATEMENT;
import static org.folio.repository.titles.TitlesTableConstants.INSERT_OR_UPDATE_TITLE_STATEMENT;
import static org.folio.repository.titles.TitlesTableConstants.SELECT_TITLES_BY_RESOURCE_TAGS;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Title;
import org.folio.repository.holdings.DbHolding;
import org.folio.repository.resources.ResourceTableConstants;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.repository.titles.DbTitle;

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

    final String query = String.format(INSERT_OR_UPDATE_TITLE_STATEMENT, getTitlesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteTitle(String titleId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(titleId));

    final String query = String.format(DELETE_TITLE_STATEMENT, getTitlesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }


  @Override
  public CompletableFuture<List<DbTitle>> getTitlesByResourceTags(List<String> tags, int page, int count, String tenantId) {
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    parameters
      .add(offset)
      .add(count);

    final String query = String.format(SELECT_TITLES_BY_RESOURCE_TAGS,
      getResourcesTableName(tenantId), getTagsTableName(tenantId), getHoldingsTableName(tenantId), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select titles by resource tags = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResult(future, this::mapTitles);
  }

  @Override
  public CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, String tenantId) {
    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    final String query = String.format(COUNT_TITLES_BY_RESOURCE_TAGS, getTagsTableName(tenantId), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select packages by tags = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResult(future, this::readTagCount);
  }

  private String createPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  private Integer readTagCount(ResultSet resultSet) {
    return resultSet.getRows().get(0).getInteger("count");
  }

  private List<DbTitle> mapTitles(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapTitle);
  }

  private DbTitle mapTitle(JsonObject row) {
    Title title = readHolding(row)
      .map(this::mapHoldingToTitle)
      .orElse(null);
    return DbTitle.builder()
      .id(Long.parseLong(row.getString("id")))
      .name(row.getString(ResourceTableConstants.NAME_COLUMN))
      .title(title)
      .build();
  }

  private Optional<DbHolding> readHolding(JsonObject row){
      if(row.getString("holding") != null) {
        return mapColumn(row, "holding", DbHolding.class);
      }
      else{
        return Optional.empty();
      }
  }

  private Title mapHoldingToTitle(DbHolding holding) {
    return Title.builder()
      .titleName(holding.getPublicationTitle())
      .titleId(Integer.parseInt(holding.getTitleId()))
      .pubType(holding.getResourceType())
      .publisherName(holding.getPublisherName())
      .build();
  }


}
