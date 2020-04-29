package org.folio.repository.accesstypes;

import static java.lang.String.format;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
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
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_FIRST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_LAST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_MIDDLE_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USERNAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPSERT_ACCESS_TYPE_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.USAGE_NUMBER_COLUMN;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.exc.ServiceExceptions;

@Component
public class AccessTypesRepositoryImpl implements AccessTypesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesRepositoryImpl.class);

  private static final String LOG_SELECT_QUERY = "Do select query = {}";
  private static final String LOG_INSERT_QUERY = "Do insert query = {}";
  private static final String LOG_COUNT_QUERY = "Do count query = {}";
  private static final String LOG_DELETE_QUERY = "Do delete query = {}";

  private static final String NAME_UNIQUENESS_MESSAGE = "Duplicate name";
  private static final String NAME_UNIQUENESS_DETAILS = "Access type with name '%s' already exist";

  @Autowired
  private DBExceptionTranslator excTranslator;

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsId(String credentialsId, String tenantId) {
    String query = format(SELECT_BY_CREDENTIALS_ID_QUERY,
      getAccessTypesTableName(tenantId), getAccessTypesMappingTableName(tenantId));

    LOG.info(LOG_SELECT_QUERY, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAccessTypes);
  }

  @Override
  public CompletableFuture<Optional<DbAccessType>> findByCredentialsAndAccessTypeId(String credentialsId,
                                                                                    String accessTypeId, String tenantId) {
    String query = String.format(SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY,
      getAccessTypesTableName(tenantId), getAccessTypesMappingTableName(tenantId));

    LOG.info(LOG_SELECT_QUERY, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Arrays.asList(accessTypeId, credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleAccessType);
  }

  @Override
  public CompletableFuture<DbAccessType> save(DbAccessType accessType, String tenantId) {
    String query = format(UPSERT_ACCESS_TYPE_QUERY, getAccessTypesTableName(tenantId));

    String id = accessType.getId();
    if (StringUtils.isBlank(accessType.getId())) {
      id = UUID.randomUUID().toString();
    }
    JsonArray params = createParams(Arrays.asList(
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

    LOG.info(LOG_INSERT_QUERY, query);

    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    Future<UpdateResult> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(uniqueNameConstraintViolation(accessType.getName()))
      .recover(credentialsNotFoundConstraintViolation(accessType.getCredentialsId()));
    return mapResult(resultFuture, setId(accessType, id));
  }

  @Override
  public CompletableFuture<Integer> count(String credentialsId, String tenantId) {
    String query = format(SELECT_COUNT_BY_CREDENTIALS_ID_QUERY, getAccessTypesTableName(tenantId));

    LOG.info(LOG_COUNT_QUERY, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()),
      rs -> rs.getResults().get(0).getInteger(0));
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String accessTypeId, String tenantId) {
    String query = format(DELETE_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY, getAccessTypesTableName(tenantId));

    LOG.info(LOG_DELETE_QUERY, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Arrays.asList(accessTypeId, credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), rs -> null);
  }

  private List<DbAccessType> mapAccessTypes(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapAccessType);
  }

  private Optional<DbAccessType> mapSingleAccessType(ResultSet resultSet) {
    List<JsonObject> rows = resultSet.getRows();
    return rows.isEmpty() ? Optional.empty() : Optional.of(mapAccessType(rows.get(0)));
  }

  private DbAccessType mapAccessType(JsonObject resultRow) {
    return DbAccessType.builder()
      .id(resultRow.getString(ID_COLUMN))
      .credentialsId(resultRow.getString(CREDENTIALS_ID_COLUMN))
      .name(resultRow.getString(NAME_COLUMN))
      .description(resultRow.getString(DESCRIPTION_COLUMN))
      .usageNumber(ObjectUtils.defaultIfNull(resultRow.getInteger(USAGE_NUMBER_COLUMN), 0))
      .createdDate(resultRow.getInstant(CREATED_DATE_COLUMN))
      .createdByUserId(resultRow.getString(CREATED_BY_USER_ID_COLUMN))
      .createdByUsername(resultRow.getString(CREATED_BY_USERNAME_COLUMN))
      .createdByLastName(resultRow.getString(CREATED_BY_LAST_NAME_COLUMN))
      .createdByFirstName(resultRow.getString(CREATED_BY_FIRST_NAME_COLUMN))
      .createdByMiddleName(resultRow.getString(CREATED_BY_MIDDLE_NAME_COLUMN))
      .updatedDate(resultRow.getInstant(UPDATED_DATE_COLUMN))
      .updatedByUserId(resultRow.getString(UPDATED_BY_USER_ID_COLUMN))
      .updatedByUsername(resultRow.getString(UPDATED_BY_USERNAME_COLUMN))
      .updatedByLastName(resultRow.getString(UPDATED_BY_LAST_NAME_COLUMN))
      .updatedByFirstName(resultRow.getString(UPDATED_BY_FIRST_NAME_COLUMN))
      .updatedByMiddleName(resultRow.getString(UPDATED_BY_MIDDLE_NAME_COLUMN))
      .build();
  }

  private Function<Throwable, Future<UpdateResult>> uniqueNameConstraintViolation(String value) {
    return uniqueConstraintRecover(NAME_COLUMN, new InputValidationException(
      NAME_UNIQUENESS_MESSAGE,
      format(NAME_UNIQUENESS_DETAILS, value)));
  }

  private Function<Throwable, Future<UpdateResult>> credentialsNotFoundConstraintViolation(String credentialsId) {
    return foreignKeyConstraintRecover(ServiceExceptions.notFound(KbCredentials.class, credentialsId));
  }

  private Function<UpdateResult, DbAccessType> setId(DbAccessType accessType, String id) {
    return updateResult -> accessType.toBuilder().id(id).build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
