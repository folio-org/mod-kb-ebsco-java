package org.folio.repository.kbcredentials;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.getAssignedUsersTableName;
import static org.folio.repository.DbUtil.getKbCredentialsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.DbUtil.uniqueConstraintRecover;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.API_KEY_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CUSTOMER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.DELETE_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_BY_ID_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_BY_USER_ID_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPSERT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.URL_COLUMN;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.BadRequestException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.DbUtil;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.persist.PostgresClient;

@Component
public class KbCredentialsRepositoryImpl implements KbCredentialsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(KbCredentialsRepositoryImpl.class);

  private static final String CREDENTIALS_NAME_UNIQUENESS_MESSAGE = "Duplicate name";
  private static final String CREDENTIALS_NAME_UNIQUENESS_DETAILS = "Credentials with name '%s' already exist";
  private static final String CREDENTIALS_CUSTOMERID_URL_UNIQUENESS_MESSAGE = "Duplicate credentials";
  private static final String CREDENTIALS_CUSTOMERID_URL_UNIQUENESS_DETAILS =
    "Credentials with customer id '%s' and url '%s' already exist";
  private static final String CREDENTIALS_DELETE_ALLOWED_DETAILS = "Credentials have related records and can't be deleted";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Collection<DbKbCredentials>> findAll(String tenant) {
    String query = prepareQuery(SELECT_CREDENTIALS_QUERY, getKbCredentialsTableName(tenant));

    logSelectQuery(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCredentialsCollection);
  }

  @Override
  public CompletableFuture<Optional<DbKbCredentials>> findById(UUID id, String tenant) {
    String query = prepareQuery(SELECT_CREDENTIALS_BY_ID_QUERY, getKbCredentialsTableName(tenant));
    Tuple params = Tuple.of(id);
    logSelectQuery(LOG, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleCredentials);
  }

  @Override
  public CompletableFuture<DbKbCredentials> save(DbKbCredentials credentials, String tenant) {
    String query = prepareQuery(UPSERT_CREDENTIALS_QUERY, getKbCredentialsTableName(tenant));

    UUID id = credentials.getId();
    if (id == null) {
      id = UUID.randomUUID();
    }
    Tuple params = createParams(asList(
      id,
      credentials.getUrl(),
      credentials.getName(),
      credentials.getApiKey(),
      credentials.getCustomerId(),
      credentials.getCreatedDate(),
      credentials.getCreatedByUserId(),
      credentials.getCreatedByUserName(),
      credentials.getUpdatedDate(),
      credentials.getUpdatedByUserId(),
      credentials.getUpdatedByUserName()
    ));

    logInsertQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(uniqueNameConstraintViolation(credentials.getName()))
      .recover(uniqueCredsConstraintViolation(credentials.getCustomerId(), credentials.getUrl()));
    return mapResult(resultFuture, setId(credentials, id));
  }

  @Override
  public CompletableFuture<Void> delete(UUID id, String tenant) {
    String query = prepareQuery(DELETE_CREDENTIALS_QUERY, getKbCredentialsTableName(tenant));
    Tuple params = Tuple.of(id);

    logDeleteQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(foreignKeyConstraintViolation());
    return mapResult(resultFuture, nothing());
  }

  @Override
  public CompletableFuture<Optional<DbKbCredentials>> findByUserId(UUID userId, String tenant) {
    String query = prepareQuery(SELECT_CREDENTIALS_BY_USER_ID_QUERY, getKbCredentialsTableName(tenant),
      getAssignedUsersTableName(tenant));
    Tuple params = Tuple.of(userId);

    logSelectQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleCredentials);
  }

  private Collection<DbKbCredentials> mapCredentialsCollection(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::mapCredentials);
  }

  private Optional<DbKbCredentials> mapSingleCredentials(RowSet<Row> resultSet) {
    return isEmpty(resultSet)
      ? Optional.empty()
      : Optional.of(mapFirstItem(resultSet, this::mapCredentials));
  }

  private DbKbCredentials mapCredentials(Row row) {
    var builder = DbKbCredentials.builder()
      .id(row.getUUID(ID_COLUMN))
      .url(row.getString(URL_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .apiKey(row.getString(API_KEY_COLUMN))
      .customerId(row.getString(CUSTOMER_ID_COLUMN))
      .updatedByUserId(row.getUUID(""));
    return DbUtil.mapMetadata(builder, row).build();
  }

  private Function<Throwable, Future<RowSet<Row>>> uniqueNameConstraintViolation(String value) {
    return uniqueConstraintRecover(NAME_COLUMN, new InputValidationException(
      CREDENTIALS_NAME_UNIQUENESS_MESSAGE,
      format(CREDENTIALS_NAME_UNIQUENESS_DETAILS, value)));
  }

  private Function<Throwable, Future<RowSet<Row>>> uniqueCredsConstraintViolation(String customerId, String url) {
    return uniqueConstraintRecover(asList(CUSTOMER_ID_COLUMN, URL_COLUMN), new InputValidationException(
      CREDENTIALS_CUSTOMERID_URL_UNIQUENESS_MESSAGE,
      format(CREDENTIALS_CUSTOMERID_URL_UNIQUENESS_DETAILS, customerId, url)));
  }

  private Function<Throwable, Future<RowSet<Row>>> foreignKeyConstraintViolation() {
    return foreignKeyConstraintRecover(new BadRequestException(CREDENTIALS_DELETE_ALLOWED_DETAILS));
  }

  private Function<RowSet<Row>, DbKbCredentials> setId(DbKbCredentials credentials, UUID id) {
    return updateResult -> credentials.toBuilder().id(id).build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
