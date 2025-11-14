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
import static org.folio.repository.DbUtil.uniqueConstraintRecover;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.API_KEY_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CUSTOMER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.URL_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.deleteCredentialsQuery;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.selectCredentialsByIdQuery;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.selectCredentialsByUserIdQuery;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.selectCredentialsQuery;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.upsertCredentialsQuery;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.ws.rs.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.DbMetadataUtil;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.persist.PostgresClient;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class KbCredentialsRepositoryImpl implements KbCredentialsRepository {
  private static final String CREDENTIALS_NAME_UNIQUENESS_MESSAGE = "Duplicate name";
  private static final String CREDENTIALS_NAME_UNIQUENESS_DETAILS = "Credentials with name '%s' already exist";
  private static final String CREDENTIALS_CUSTOMERID_URL_UNIQUENESS_MESSAGE = "Duplicate credentials";
  private static final String CREDENTIALS_CUSTOMERID_URL_UNIQUENESS_DETAILS =
    "Credentials with customer id '%s' and url '%s' already exist";
  private static final String CREDENTIALS_DELETE_ALLOWED_DETAILS =
    "Credentials have related records and can't be deleted";

  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public KbCredentialsRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Collection<DbKbCredentials>> findAll(String tenant) {
    String query = selectCredentialsQuery(tenant);

    logSelectQuery(log, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, promise::handle);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCredentialsCollection);
  }

  @Override
  public CompletableFuture<Optional<DbKbCredentials>> findById(UUID id, String tenant) {
    String query = selectCredentialsByIdQuery(tenant);
    Tuple params = Tuple.of(id);
    logSelectQuery(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, params, promise::handle);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleCredentials);
  }

  @Override
  public CompletableFuture<DbKbCredentials> save(DbKbCredentials credentials, String tenant) {
    String query = upsertCredentialsQuery(tenant);

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
      credentials.getCreatedByUserName() == null ? "SYSTEM" : credentials.getCreatedByUserName(),
      credentials.getUpdatedDate(),
      credentials.getUpdatedByUserId(),
      credentials.getUpdatedByUserName() == null ? "SYSTEM" : credentials.getUpdatedByUserName()
    ));

    logInsertQuery(log, query, params, true);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise::handle);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(uniqueNameConstraintViolation(credentials.getName()))
      .recover(uniqueCredsConstraintViolation(credentials.getCustomerId(), credentials.getUrl()));
    return mapResult(resultFuture, setId(credentials, id));
  }

  @Override
  public CompletableFuture<Void> delete(UUID id, String tenant) {
    String query = deleteCredentialsQuery(tenant);
    Tuple params = Tuple.of(id);

    logDeleteQuery(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise::handle);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(foreignKeyConstraintViolation());
    return mapResult(resultFuture, nothing());
  }

  @Override
  public CompletableFuture<Optional<DbKbCredentials>> findByUserId(UUID userId, String tenant) {
    String query = selectCredentialsByUserIdQuery(tenant);
    Tuple params = Tuple.of(userId);

    logSelectQuery(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, params, promise::handle);

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
      .customerId(row.getString(CUSTOMER_ID_COLUMN));
    return DbMetadataUtil.mapMetadata(builder, row).build();
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
