package org.folio.repository.accesstypes;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.LogUtils.logCountQuery;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.uniqueConstraintRecover;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.DESCRIPTION_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.USAGE_NUMBER_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.deleteByCredentialsAndAccessTypeIdQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.selectByCredentialsAndAccessTypeIdQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.selectByCredentialsAndNamesQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.selectByCredentialsAndRecordIdsQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.selectByCredentialsAndRecordQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.selectByCredentialsIdWithCountQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.selectCountByCredentialsIdQuery;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.upsertAccessTypeQuery;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordType;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.exc.ServiceExceptions;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class AccessTypesRepositoryImpl implements AccessTypesRepository {

  private static final String NAME_UNIQUENESS_MESSAGE = "Duplicate name";
  private static final String NAME_UNIQUENESS_DETAILS = "Access type with name '%s' already exist";

  private final DBExceptionTranslator excTranslator;

  private final Vertx vertx;

  public AccessTypesRepositoryImpl(DBExceptionTranslator excTranslator, Vertx vertx) {
    this.excTranslator = excTranslator;
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsId(UUID credentialsId, String tenantId) {
    String query = selectByCredentialsIdWithCountQuery(tenantId);
    Tuple params = createParams(credentialsId);

    logSelectQueryInfoLevel(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessTypes);
  }

  @Override
  public CompletableFuture<Optional<DbAccessType>> findByCredentialsAndAccessTypeId(UUID credentialsId,
                                                                                    UUID accessTypeId,
                                                                                    String tenantId) {
    String query = selectByCredentialsAndAccessTypeIdQuery(tenantId);
    Tuple params = createParams(accessTypeId, credentialsId);

    logSelectQueryInfoLevel(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessType);
  }

  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsAndNames(UUID credentialsId,
                                                                         Collection<String> accessTypeNames,
                                                                         String tenantId) {
    String query = selectByCredentialsAndNamesQuery(accessTypeNames, tenantId);
    Tuple params = Tuple.tuple();
    params.addUUID(credentialsId);
    accessTypeNames.forEach(params::addString);

    logSelectQueryInfoLevel(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessTypes);
  }

  @Override
  public CompletableFuture<Optional<DbAccessType>> findByCredentialsAndRecord(UUID credentialsId, String recordId,
                                                                              RecordType recordType,
                                                                              String tenantId) {
    String query = selectByCredentialsAndRecordQuery(tenantId);
    Tuple params = createParams(credentialsId, recordId, recordType.getValue());

    logSelectQueryInfoLevel(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessType);
  }

  @Override
  public CompletableFuture<DbAccessType> save(DbAccessType accessType, String tenantId) {
    String query = upsertAccessTypeQuery(tenantId);

    UUID id = accessType.getId();
    if (id == null) {
      id = UUID.randomUUID();
    }
    Tuple params = createParams(
      id,
      accessType.getCredentialsId(),
      accessType.getName(),
      accessType.getDescription(),
      accessType.getCreatedDate(),
      accessType.getCreatedByUserId(),
      accessType.getUpdatedDate(),
      accessType.getUpdatedByUserId()
    );

    logInsertQueryInfoLevel(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(uniqueNameConstraintViolation(accessType.getName()))
      .recover(credentialsNotFoundConstraintViolation(accessType.getCredentialsId()));
    return mapResult(resultFuture, setId(accessType, id));
  }

  @Override
  public CompletableFuture<Integer> count(UUID credentialsId, String tenantId) {
    String query = selectCountByCredentialsIdQuery(tenantId);
    Tuple params = createParams(credentialsId);

    logCountQuery(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()),
      rs -> mapFirstItem(rs, row -> row.getInteger(0)));
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, UUID accessTypeId, String tenantId) {
    String query = deleteByCredentialsAndAccessTypeIdQuery(tenantId);
    Tuple params = createParams(accessTypeId, credentialsId);

    logDeleteQueryInfoLevel(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), rs -> null);
  }

  @Override
  public CompletionStage<Map<String, DbAccessType>> findPerRecord(String credentialsId, List<String> recordIds,
                                                                  RecordType recordType, String tenant) {
    if (isEmpty(recordIds)) {
      return completedFuture(Collections.emptyMap());
    }
    Future<RowSet<Row>> resultSetFuture = findByRecordIdsOfType(credentialsId, recordIds, recordType, tenant);

    return mapResult(resultSetFuture, this::mapAccessTypesPerRecord);
  }

  private Future<RowSet<Row>> findByRecordIdsOfType(String credentialsId, List<String> recordIds, RecordType recordType,
                                                    String tenantId) {
    Tuple parameters = createParametersWithRecordType(credentialsId, recordIds, recordType);

    String query = selectByCredentialsAndRecordIdsQuery(recordIds, tenantId);
    logSelectQueryInfoLevel(log, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return promise.future().recover(excTranslator.translateOrPassBy());
  }

  private Tuple createParametersWithRecordType(String credentialsId, List<String> queryParameters,
                                               RecordType recordType) {
    Tuple parameters = Tuple.tuple();
    parameters.addString(credentialsId);
    parameters.addString(recordType.getValue());
    queryParameters.forEach(parameters::addString);
    return parameters;
  }

  private Map<String, DbAccessType> mapAccessTypesPerRecord(RowSet<Row> resultSet) {
    return RowSetUtils.streamOf(resultSet)
      .map(row -> {
        String recordId = row.getString(AccessTypeMappingsTableConstants.RECORD_ID_COLUMN);
        DbAccessType accessType = mapAccessType(row);
        return ImmutablePair.of(recordId, accessType);
      })
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  private List<DbAccessType> mapAccessTypes(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::mapAccessType);
  }

  private Optional<DbAccessType> mapSingleAccessType(RowSet<Row> resultSet) {
    return resultSet.rowCount() == 0 ? Optional.empty() : Optional.of(mapFirstItem(resultSet, this::mapAccessType));
  }

  private DbAccessType mapAccessType(Row row) {
    return DbAccessType.builder()
      .id(row.getUUID(ID_COLUMN))
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .description(row.getString(DESCRIPTION_COLUMN))
      .usageNumber(getIntValueOrDefault(row, USAGE_NUMBER_COLUMN, 0))
      .createdDate(row.getOffsetDateTime(CREATED_DATE_COLUMN))
      .createdByUserId(row.getUUID(CREATED_BY_USER_ID_COLUMN))
      .updatedDate(row.getOffsetDateTime(UPDATED_DATE_COLUMN))
      .updatedByUserId(row.getUUID(UPDATED_BY_USER_ID_COLUMN))
      .build();
  }

  private int getIntValueOrDefault(Row row, String columnName, int defaultValue) {
    return row.getColumnIndex(columnName) != -1
           ? ObjectUtils.defaultIfNull(row.getInteger(columnName), defaultValue)
           : defaultValue;
  }

  private Function<Throwable, Future<RowSet<Row>>> uniqueNameConstraintViolation(String value) {
    return uniqueConstraintRecover(asList(CREDENTIALS_ID_COLUMN, NAME_COLUMN), new InputValidationException(
      NAME_UNIQUENESS_MESSAGE,
      format(NAME_UNIQUENESS_DETAILS, value)));
  }

  private Function<Throwable, Future<RowSet<Row>>> credentialsNotFoundConstraintViolation(UUID credentialsId) {
    return foreignKeyConstraintRecover(ServiceExceptions.notFound(KbCredentials.class, credentialsId.toString()));
  }

  private Function<RowSet<Row>, DbAccessType> setId(DbAccessType accessType, UUID id) {
    return updateResult -> accessType.toBuilder().id(id).build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
