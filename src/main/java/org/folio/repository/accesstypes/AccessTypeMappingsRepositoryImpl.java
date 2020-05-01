package org.folio.repository.accesstypes;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.COUNT_BY_ACCESS_TYPE_AND_RECORD_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.COUNT_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.DELETE_BY_RECORD_ID_AND_RECORD_TYPE_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_BY_ACCESS_TYPE_IDS_AND_RECORD_TYPE_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_BY_ACCESS_TYPE_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_BY_RECORD_ID_AND_RECORD_TYPE_QUERY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.UPSERT_QUERY;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import org.folio.repository.RecordType;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypeMappingsRepositoryImpl implements AccessTypeMappingsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypeMappingsRepositoryImpl.class);

  private static final String DELETE_LOG_MESSAGE = "Do delete query = {}";
  private static final String INSERT_LOG_MESSAGE = "Do insert query = {}";
  private static final String SELECT_LOG_MESSAGE = "Do select query = {}";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType,
                                                                     String credentialsId, String tenantId) {
    String query = format(SELECT_BY_RECORD_ID_AND_RECORD_TYPE_QUERY, getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    JsonArray params = createParams(asList(recordId, recordType.getValue(), credentialsId));
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessItem);
  }

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeId(String accessTypeId, String tenantId) {
    JsonArray params = createParams(singletonList(accessTypeId));
    String query = format(SELECT_BY_ACCESS_TYPE_ID_QUERY, getAccessTypesMappingTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessItemCollection);
  }

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                                 String tenantId) {
    List<String> accessTypeIds = accessTypeFilter.getAccessTypeIds();
    if (CollectionUtils.isEmpty(accessTypeIds)) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int page = accessTypeFilter.getPage();
    int count = accessTypeFilter.getCount();
    int offset = (page - 1) * count;

    JsonArray params = createParams(accessTypeIds);
    params.add(accessTypeFilter.getRecordType().getValue());
    params.add(accessTypeFilter.getRecordIdPrefix() + "%");
    params.add(offset);
    params.add(count);

    String query = format(SELECT_BY_ACCESS_TYPE_IDS_AND_RECORD_TYPE_QUERY, getAccessTypesMappingTableName(tenantId),
      createPlaceholders(accessTypeIds.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessItemCollection);
  }

  @Override
  public CompletableFuture<AccessTypeMapping> save(AccessTypeMapping mapping, String tenantId) {

    JsonArray params = createUpsertParams(mapping);
    String query = format(UPSERT_QUERY, getAccessTypesMappingTableName(tenantId));

    LOG.info(INSERT_LOG_MESSAGE, query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> mapping);
  }

  @Override
  public CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, String credentialsId,
                                                String tenantId) {
    String query = format(DELETE_BY_RECORD_ID_AND_RECORD_TYPE_QUERY, getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    JsonArray params = createParams(asList(recordId, recordType.getValue(), credentialsId));
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> null);
  }

  @Override
  public CompletableFuture<Map<String, Integer>> countRecordsByAccessTypeAndRecordIdPrefix(String recordIdPrefix,
                                                                                           RecordType recordType,
                                                                                           String credentialsId,
                                                                                           String tenantId) {
    String query = format(COUNT_BY_ACCESS_TYPE_AND_RECORD_QUERY, getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    JsonArray params = createParams(asList(recordIdPrefix + "%", recordType.getValue(), credentialsId));
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCount);
  }

  private Map<String, Integer> mapCount(ResultSet resultSet) {
    return resultSet.getRows().stream()
      .collect(Collectors.toMap(row -> row.getString(ACCESS_TYPE_ID_COLUMN), row -> row.getInteger(COUNT_COLUMN)));
  }

  private List<AccessTypeMapping> mapAccessItemCollection(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapAccessItem);
  }

  private Optional<AccessTypeMapping> mapSingleAccessItem(ResultSet resultSet) {
    final List<JsonObject> rows = resultSet.getRows();

    return rows.isEmpty() ? Optional.empty() : Optional.of(mapAccessItem(rows.get(0)));
  }

  private AccessTypeMapping mapAccessItem(JsonObject row) {
    return AccessTypeMapping.builder()
      .id(row.getString(ID_COLUMN))
      .accessTypeId(row.getString(ACCESS_TYPE_ID_COLUMN))
      .recordId(row.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN)))
      .build();
  }

  private JsonArray createUpsertParams(AccessTypeMapping mapping) {
    return createParams(asList(
      mapping.getId(),
      mapping.getRecordId(),
      mapping.getRecordType().getValue(),
      mapping.getAccessTypeId()
    ));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
