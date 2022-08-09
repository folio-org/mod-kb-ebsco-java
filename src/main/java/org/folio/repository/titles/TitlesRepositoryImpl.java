package org.folio.repository.titles;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.holdings.HoldingsTableConstants.PACKAGE_ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.PUBLICATION_TITLE_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.PUBLISHER_NAME_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.RESOURCE_TYPE_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.TITLE_ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.VENDOR_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.NAME_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.COUNT_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.HOLDINGS_ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.deleteTitleStatement;
import static org.folio.repository.titles.TitlesTableConstants.getCountTitlesByResourceTags;
import static org.folio.repository.titles.TitlesTableConstants.insertOrUpdateTitleStatement;
import static org.folio.repository.titles.TitlesTableConstants.selectTitlesByResourceTags;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.persist.PostgresClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TitlesRepositoryImpl implements TitlesRepository {

  private static final Logger LOG = LogManager.getLogger(TitlesRepositoryImpl.class);

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

    final String query = insertOrUpdateTitleStatement(tenantId);

    logInsertQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(Long titleId, UUID credentialsId, String tenantId) {
    Tuple params = createParams(String.valueOf(titleId), credentialsId);

    final String query = deleteTitleStatement(tenantId);

    logDeleteQueryInfoLevel(LOG, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, UUID credentialsId, String tenantId) {

    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(0);
    }

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    parameters.addUUID(credentialsId);

    final String query = getCountTitlesByResourceTags(tenantId, tags);

    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagCount);
  }

  @Override
  public CompletableFuture<List<DbTitle>> findByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId) {
    List<String> tags = tagFilter.getTags();
    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    parameters
      .addUUID(credentialsId)
      .addInteger(tagFilter.getOffset())
      .addInteger(tagFilter.getCount());

    final String query = selectTitlesByResourceTags(tenantId, tags);

    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapTitles);
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
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .title(title)
      .build();
  }

  private Optional<DbHoldingInfo> readHolding(Row row) {
    if (row.getUUID(CREDENTIALS_ID_COLUMN) != null && row.getString(HOLDINGS_ID_COLUMN) != null) {
      DbHoldingInfo holdingInfo = DbHoldingInfo.builder()
        .titleId(row.getInteger(TITLE_ID_COLUMN))
        .packageId(row.getInteger(PACKAGE_ID_COLUMN))
        .vendorId(row.getInteger(VENDOR_ID_COLUMN))
        .publicationTitle(row.getString(PUBLICATION_TITLE_COLUMN))
        .publisherName(row.getString(PUBLISHER_NAME_COLUMN))
        .resourceType(row.getString(RESOURCE_TYPE_COLUMN))
        .build();
      return Optional.of(holdingInfo);
    } else {
      return Optional.empty();
    }
  }

  private Title mapHoldingToTitle(DbHoldingInfo holding) {
    return Title.builder()
      .titleName(holding.getPublicationTitle())
      .titleId(holding.getTitleId())
      .pubType(holding.getResourceType())
      .publisherName(holding.getPublisherName())
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
