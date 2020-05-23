package org.folio.repository.titles;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
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
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.resources.ResourceTableConstants;
import org.folio.rest.persist.PostgresClient;

@Component
public class TitlesRepositoryImpl implements TitlesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TitlesRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;


  @Override
  public CompletableFuture<Void> save(DbTitle title, String tenantId) {
    JsonArray parameters = createInsertOrUpdateParameters(String.valueOf(title.getId()), title.getCredentialsId(),
      title.getName());

    final String query = String.format(INSERT_OR_UPDATE_TITLE_STATEMENT, getTitlesTableName(tenantId));

    LOG.info(INSERT_LOG_MESSAGE, query);

    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(Long titleId, String credentialsId, String tenantId) {
    JsonArray parameter = createParams(asList(String.valueOf(titleId), credentialsId));

    final String query = String.format(DELETE_TITLE_STATEMENT, getTitlesTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameter, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<DbTitle>> getTitlesByResourceTags(List<String> tags, int page, int count,
      String credentialsId, String tenantId) {

    if(CollectionUtils.isEmpty(tags)){
      return completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    parameters
      .add(credentialsId)
      .add(offset)
      .add(count);

    final String query = String.format(SELECT_TITLES_BY_RESOURCE_TAGS,
      getResourcesTableName(tenantId), getTagsTableName(tenantId), getHoldingsTableName(tenantId),
      createPlaceholders(tags.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapTitles);
  }

  @Override
  public CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, String credentialsId,
      String tenantId) {

    if(CollectionUtils.isEmpty(tags)){
      return completedFuture(0);
    }

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    parameters.add(credentialsId);

    final String query = String.format(COUNT_TITLES_BY_RESOURCE_TAGS,
      getResourcesTableName(tenantId), getTagsTableName(tenantId), createPlaceholders(tags.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagCount);
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
      .id(Long.parseLong(row.getString(ResourceTableConstants.ID_COLUMN)))
      .name(row.getString(ResourceTableConstants.CREDENTIALS_ID_COLUMN))
      .name(row.getString(ResourceTableConstants.NAME_COLUMN))
      .title(title)
      .build();
  }

  private Optional<HoldingInfoInDB> readHolding(JsonObject row) {
    if (row.getString("holding") != null) {
      return mapColumn(row, "holding", HoldingInfoInDB.class);
    } else {
      return Optional.empty();
    }
  }

  private Title mapHoldingToTitle(HoldingInfoInDB holding) {
    return Title.builder()
      .titleName(holding.getPublicationTitle())
      .titleId(Integer.parseInt(holding.getTitleId()))
      .pubType(holding.getResourceType())
      .publisherName(holding.getPublisherName())
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
