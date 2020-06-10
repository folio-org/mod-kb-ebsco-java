package org.folio.repository.accesstypes;

import static java.util.Arrays.asList;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.streamOf;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.COUNT_BY_RECORD_ID_PREFIX_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.COUNT_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.DELETE_BY_RECORD_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_BY_ACCESS_TYPE_IDS_AND_RECORD_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_BY_RECORD_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.UPSERT_QUERY;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordType;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypeMappingsRepositoryImpl implements AccessTypeMappingsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypeMappingsRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType,
                                                                     UUID credentialsId, String tenantId) {
    String query = prepareQuery(SELECT_BY_RECORD_QUERY, getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Tuple params = createParams(asList(recordId, recordType.getValue(), credentialsId));
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessItem);
  }

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                                 String tenantId) {
    List<UUID> accessTypeIds = accessTypeFilter.getAccessTypeIds();
    if (CollectionUtils.isEmpty(accessTypeIds)) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int page = accessTypeFilter.getPage();
    int count = accessTypeFilter.getCount();
    int offset = (page - 1) * count;

    Tuple params = createParams(accessTypeIds);
    params.addString(accessTypeFilter.getRecordType().getValue());
    params.addString(accessTypeFilter.getRecordIdPrefix() + "%");
    params.addInteger(offset);
    params.addInteger(count);

    String query = prepareQuery(SELECT_BY_ACCESS_TYPE_IDS_AND_RECORD_QUERY, getAccessTypesMappingTableName(tenantId),
      createPlaceholders(accessTypeIds.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessItemCollection);
  }

  @Override
  public CompletableFuture<AccessTypeMapping> save(AccessTypeMapping mapping, String tenantId) {
    String query = prepareQuery(UPSERT_QUERY, getAccessTypesMappingTableName(tenantId));

    Tuple params = createParams(asList(
      mapping.getId(),
      mapping.getRecordId(),
      mapping.getRecordType().getValue(),
      mapping.getAccessTypeId()
    ));

    LOG.info(INSERT_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> mapping);
  }

  @Override
  public CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, UUID credentialsId,
                                                String tenantId) {
    String query = prepareQuery(DELETE_BY_RECORD_QUERY, getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    Tuple params = createParams(asList(recordId, recordType.getValue(), credentialsId));
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Map<UUID, Integer>> countByRecordIdPrefix(String recordIdPrefix, RecordType recordType,
                                                                     UUID credentialsId, String tenantId) {
    String query = prepareQuery(COUNT_BY_RECORD_ID_PREFIX_QUERY, getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Tuple params = createParams(asList(recordIdPrefix + "%", recordType.getValue(), credentialsId));
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCount);
  }

  private Map<UUID, Integer> mapCount(RowSet<Row> resultSet) {
    return streamOf(resultSet)
      .collect(Collectors.toMap(row -> row.getUUID(ACCESS_TYPE_ID_COLUMN), row -> row.getInteger(COUNT_COLUMN)));
  }

  private List<AccessTypeMapping> mapAccessItemCollection(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::mapAccessItem);
  }

  private Optional<AccessTypeMapping> mapSingleAccessItem(RowSet<Row> resultSet) {
    return isEmpty(resultSet) ? Optional.empty() : Optional.of(mapFirstItem(resultSet, this::mapAccessItem));
  }

  private AccessTypeMapping mapAccessItem(Row row) {
    return AccessTypeMapping.builder()
      .id(row.getUUID(ID_COLUMN))
      .accessTypeId(row.getUUID(ACCESS_TYPE_ID_COLUMN))
      .recordId(row.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN)))
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
