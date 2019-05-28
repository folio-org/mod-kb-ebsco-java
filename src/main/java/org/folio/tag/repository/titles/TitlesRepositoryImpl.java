package org.folio.tag.repository.titles;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.tag.repository.DbUtil.mapResultSet;
import static org.folio.tag.repository.DbUtil.mapVertxFuture;
import static org.folio.tag.repository.resources.HoldingsTableConstants.HOLDINGS_TABLE_NAME;
import static org.folio.tag.repository.titles.TitlesTableConstants.COUNT_TITLES_BY_RESOURCE_TAGS;
import static org.folio.tag.repository.titles.TitlesTableConstants.DELETE_TITLE_STATEMENT;
import static org.folio.tag.repository.titles.TitlesTableConstants.INSERT_OR_UPDATE_TITLE_STATEMENT;
import static org.folio.tag.repository.titles.TitlesTableConstants.SELECT_TITLES_BY_RESOURCE_TAGS;
import static org.folio.tag.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Holding;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ObjectMapperTool;
import org.folio.tag.repository.DbUtil;
import org.folio.tag.repository.TagTableConstants;
import org.folio.tag.repository.resources.ResourceTableConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

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
    JsonArray parameters = DbUtil.createInsertOrUpdateParameters(String.valueOf(title.getTitleId()), title.getTitleName());

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

  @Override
  public CompletableFuture<List<DbTitle>> getTitleIdsByResourceTags(List<String> tags, int page, int count, String tenant) {
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    parameters
      .add(offset)
      .add(count);

    final String query = String.format(SELECT_TITLES_BY_RESOURCE_TAGS,
      getTableName(tenant), getTagsTableName(tenant), getHoldingsTableName(tenant), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenant);

    LOG.info("Select titles by resource tags = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResultSet(future, this::mapTitles);
  }

  @Override
  public CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, String tenant) {
    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    final String query = String.format(COUNT_TITLES_BY_RESOURCE_TAGS, getTagsTableName(tenant), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenant);

    LOG.info("Select packages by tags = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResultSet(future, this::readTagCount);
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

  private Optional<Holding> readHolding(JsonObject row){
    try {
      return Optional.of(ObjectMapperTool.getMapper().readValue(row.getString("holding"), Holding.class));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private Title mapHoldingToTitle(Holding holding) {
    return Title.builder()
      .titleName(holding.getPublicationTitle())
      .titleId(Integer.parseInt(holding.getTitleId()))
      .pubType(holding.getPublicationType())
      .publisherName(holding.getPublisherName())
      .build();
  }

  private String getTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + TITLES_TABLE_NAME;
  }

  private String getTagsTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + TagTableConstants.TABLE_NAME;
  }

  private String getHoldingsTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + HOLDINGS_TABLE_NAME;
  }
}
