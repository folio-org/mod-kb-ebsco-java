package org.folio.repository.accesstypes;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.DbUtil.COUNT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.DbUtil.uniqueConstraintRecover;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_FIRST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_LAST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_MIDDLE_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_USERNAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.DELETE_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.DESCRIPTION_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_BY_CREDENTIALS_AND_NAMES_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_BY_CREDENTIALS_AND_RECORD_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_BY_CREDENTIALS_ID_WITH_COUNT_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_FIRST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_LAST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_MIDDLE_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USERNAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPSERT_ACCESS_TYPE_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.USAGE_NUMBER_COLUMN;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.ListUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordType;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.exc.ServiceExceptions;

@Component
public class AccessTypesRepositoryImpl implements AccessTypesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesRepositoryImpl.class);

  private static final String NAME_UNIQUENESS_MESSAGE = "Duplicate name";
  private static final String NAME_UNIQUENESS_DETAILS = "Access type with name '%s' already exist";

  @Autowired
  private DBExceptionTranslator excTranslator;

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsId(UUID credentialsId, String tenantId) {
    String query = prepareQuery(SELECT_BY_CREDENTIALS_ID_WITH_COUNT_QUERY,
      getAccessTypesTableName(tenantId), getAccessTypesMappingTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessTypes);
  }

  @Override
  public CompletableFuture<Optional<DbAccessType>> findByCredentialsAndAccessTypeId(UUID credentialsId,
                                                                                    UUID accessTypeId, String tenantId) {
    String query = prepareQuery(SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY,
      getAccessTypesTableName(tenantId), getAccessTypesMappingTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(asList(accessTypeId, credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessType);
  }

  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsAndNames(UUID credentialsId,
                                                                         Collection<String> accessTypeNames,
                                                                         String tenantId) {
    String query = prepareQuery(SELECT_BY_CREDENTIALS_AND_NAMES_QUERY,
      getAccessTypesTableName(tenantId), ListUtils.createPlaceholders(accessTypeNames.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Tuple params = Tuple.tuple();
    params.addUUID(credentialsId);
    accessTypeNames.forEach(params::addString);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessTypes);
  }

  @Override
  public CompletableFuture<Optional<DbAccessType>> findByCredentialsAndRecord(UUID credentialsId, String recordId,
                                                                              RecordType recordType,
                                                                              String tenantId) {
    String query = prepareQuery(SELECT_BY_CREDENTIALS_AND_RECORD_QUERY,
      getAccessTypesTableName(tenantId), getAccessTypesMappingTableName(tenantId));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Tuple params = createParams(asList(credentialsId, recordId, recordType.getValue()));
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessType);
  }

  @Override
  public CompletableFuture<DbAccessType> save(DbAccessType accessType, String tenantId) {
    String query = prepareQuery(UPSERT_ACCESS_TYPE_QUERY, getAccessTypesTableName(tenantId));

    UUID id = accessType.getId();
    if (id == null) {
      id = UUID.randomUUID();
    }
    Tuple params = createParams(asList(
      id,
      accessType.getCredentialsId(),
      accessType.getName(),
      accessType.getDescription(),
      accessType.getCreatedDate(),
      accessType.getCreatedByUserId(),
      accessType.getCreatedByUsername(),
      accessType.getCreatedByLastName(),
      accessType.getCreatedByFirstName(),
      accessType.getCreatedByMiddleName(),
      accessType.getUpdatedDate(),
      accessType.getUpdatedByUserId(),
      accessType.getCreatedByUsername(),
      accessType.getUpdatedByLastName(),
      accessType.getUpdatedByFirstName(),
      accessType.getUpdatedByMiddleName()
    ));

    LOG.info(INSERT_LOG_MESSAGE, query);

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
    String query = prepareQuery(SELECT_COUNT_BY_CREDENTIALS_ID_QUERY, getAccessTypesTableName(tenantId));

    LOG.info(COUNT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()),
      rs -> mapFirstItem(rs, row -> row.getInteger(0)));
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, UUID accessTypeId, String tenantId) {
    String query = prepareQuery(DELETE_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY, getAccessTypesTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(asList(accessTypeId, credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), rs -> null);
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
      .usageNumber(ObjectUtils.defaultIfNull(row.getInteger(USAGE_NUMBER_COLUMN), 0))
      .createdDate(row.getOffsetDateTime(CREATED_DATE_COLUMN))
      .createdByUserId(row.getUUID(CREATED_BY_USER_ID_COLUMN))
      .createdByUsername(row.getString(CREATED_BY_USERNAME_COLUMN))
      .createdByLastName(row.getString(CREATED_BY_LAST_NAME_COLUMN))
      .createdByFirstName(row.getString(CREATED_BY_FIRST_NAME_COLUMN))
      .createdByMiddleName(row.getString(CREATED_BY_MIDDLE_NAME_COLUMN))
      .updatedDate(row.getOffsetDateTime(UPDATED_DATE_COLUMN))
      .updatedByUserId(row.getUUID(UPDATED_BY_USER_ID_COLUMN))
      .updatedByUsername(row.getString(UPDATED_BY_USERNAME_COLUMN))
      .updatedByLastName(row.getString(UPDATED_BY_LAST_NAME_COLUMN))
      .updatedByFirstName(row.getString(UPDATED_BY_FIRST_NAME_COLUMN))
      .updatedByMiddleName(row.getString(UPDATED_BY_MIDDLE_NAME_COLUMN))
      .build();
  }

  private Function<Throwable, Future<RowSet<Row>>> uniqueNameConstraintViolation(String value) {
    return uniqueConstraintRecover(NAME_COLUMN, new InputValidationException(
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
