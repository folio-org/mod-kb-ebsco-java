package org.folio.repository.titles;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.getTitlesTableName;
import static org.folio.repository.DbUtil.mapRow;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.resources.ResourceTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.NAME_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.COUNT_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.COUNT_TITLES_BY_RESOURCE_TAGS;
import static org.folio.repository.titles.TitlesTableConstants.DELETE_TITLE_STATEMENT;
import static org.folio.repository.titles.TitlesTableConstants.HOLDINGS_ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.INSERT_OR_UPDATE_TITLE_STATEMENT;
import static org.folio.repository.titles.TitlesTableConstants.SELECT_TITLES_BY_RESOURCE_TAGS;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.holdings.DbHoldingInfo;
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
    Tuple parameters = createParams(asList(
      String.valueOf(title.getId()),
      title.getCredentialsId(),
      title.getName(),
      title.getName()
    ));

    final String query = prepareQuery(INSERT_OR_UPDATE_TITLE_STATEMENT, getTitlesTableName(tenantId));

    LOG.info(INSERT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(Long titleId, String credentialsId, String tenantId) {
    Tuple parameter = createParams(asList(String.valueOf(titleId), credentialsId));

    final String query = prepareQuery(DELETE_TITLE_STATEMENT, getTitlesTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameter, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<DbTitle>> getTitlesByResourceTags(List<String> tags, int page, int count,
                                                                  String credentialsId, String tenantId) {

    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    parameters
      .addString(credentialsId)
      .addInteger(offset)
      .addInteger(count);

    final String query = prepareQuery(SELECT_TITLES_BY_RESOURCE_TAGS,
      getResourcesTableName(tenantId), getTagsTableName(tenantId), getHoldingsTableName(tenantId),
      createPlaceholders(tags.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapTitles);
  }

  @Override
  public CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, String credentialsId,
                                                              String tenantId) {

    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(0);
    }

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    parameters.addString(credentialsId);

    final String query = prepareQuery(COUNT_TITLES_BY_RESOURCE_TAGS,
      getResourcesTableName(tenantId), getTagsTableName(tenantId), createPlaceholders(tags.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagCount);
  }

  private Integer readTagCount(RowSet<Row> resultSet) {
    return RowSetUtils.mapFirstItem(resultSet, row -> row.getInteger(COUNT_COLUMN));
  }

  private List<DbTitle> mapTitles(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, this::mapTitle);
  }

  private DbTitle mapTitle(Row row) {
    Title title = readHolding(row)
      .map(this::mapHoldingToTitle)
      .orElse(null);
    return DbTitle.builder()
      .id(Long.parseLong(row.getString(ID_COLUMN)))
      .credentialsId(row.getString(CREDENTIALS_ID_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .title(title)
      .build();
  }

  private Optional<DbHoldingInfo> readHolding(Row row) {
    if (row.getString(CREDENTIALS_ID_COLUMN) != null && row.getString(HOLDINGS_ID_COLUMN) != null) {
      return mapRow(row, DbHoldingInfo.class);
    } else {
      return Optional.empty();
    }
  }

  private Title mapHoldingToTitle(DbHoldingInfo holding) {
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
