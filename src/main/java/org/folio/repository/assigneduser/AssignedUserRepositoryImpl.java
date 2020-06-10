package org.folio.repository.assigneduser;

import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.DbUtil.COUNT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.UPDATE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.getAssignedUsersTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.DbUtil.pkConstraintRecover;
import static org.folio.repository.assigneduser.AssignedUsersConstants.CREDENTIALS_ID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.DELETE_ASSIGNED_USER_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.FIRST_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.INSERT_ASSIGNED_USER_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.LAST_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.MIDDLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.PATRON_GROUP;
import static org.folio.repository.assigneduser.AssignedUsersConstants.SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.UPDATE_ASSIGNED_USER_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.USER_NAME;
import static org.folio.service.exc.ServiceExceptions.notFound;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;

@Component
public class AssignedUserRepositoryImpl implements AssignedUserRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AssignedUserRepositoryImpl.class);

  private static final String USER_ASSIGN_NOT_ALLOWED_MESSAGE = "The user is already assigned to another credentials";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Collection<DbAssignedUser>> findByCredentialsId(UUID credentialsId, String tenant) {
    String query = prepareQuery(SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY, getAssignedUsersTableName(tenant));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAssignedUserCollection);
  }

  @Override
  public CompletableFuture<Integer> count(UUID credentialsId, String tenant) {
    String query = prepareQuery(SELECT_COUNT_BY_CREDENTIALS_ID_QUERY, getAssignedUsersTableName(tenant));

    LOG.info(COUNT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, Tuple.of(credentialsId), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()),
      rs -> mapFirstItem(rs, row -> row.getInteger(0)));
  }

  @Override
  public CompletableFuture<DbAssignedUser> save(DbAssignedUser entity, String tenant) {
    String query = prepareQuery(INSERT_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    Tuple params = createParams(Arrays.asList(
      entity.getId(),
      entity.getCredentialsId(),
      entity.getUsername(),
      entity.getFirstName(),
      entity.getMiddleName(),
      entity.getLastName(),
      entity.getPatronGroup()
    ));

    LOG.info(INSERT_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(pkConstraintRecover(ID_COLUMN, new BadRequestException(USER_ASSIGN_NOT_ALLOWED_MESSAGE)))
      .recover(foreignKeyConstraintRecover(notFound(KbCredentials.class, entity.getCredentialsId().toString())));
    return mapResult(resultFuture, updateResult -> entity);
  }

  @Override
  public CompletableFuture<Void> update(DbAssignedUser dbAssignedUser, String tenant) {
    String query = prepareQuery(UPDATE_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    Tuple params = createParams(Arrays.asList(
      dbAssignedUser.getUsername(),
      dbAssignedUser.getFirstName(),
      dbAssignedUser.getMiddleName(),
      dbAssignedUser.getLastName(),
      dbAssignedUser.getPatronGroup(),
      dbAssignedUser.getId(),
      dbAssignedUser.getCredentialsId()
    ));

    LOG.info(UPDATE_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy());
    return mapResult(resultFuture, updateResult -> checkUserFound(dbAssignedUser.getId(), updateResult));
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, UUID userId, String tenant) {
    String query = prepareQuery(DELETE_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    LOG.info(DELETE_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, createParams(Arrays.asList(credentialsId, userId)), promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy());
    return mapResult(resultFuture, updateResult -> checkUserFound(userId, updateResult));
  }

  private Void checkUserFound(UUID userId, RowSet<Row> rowSet) {
    if (isEmpty(rowSet)) {
      throw notFound("Assigned User", userId.toString());
    }
    return null;
  }

  private Collection<DbAssignedUser> mapAssignedUserCollection(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::mapAssignedUserItem);
  }

  private DbAssignedUser mapAssignedUserItem(Row row) {
    return DbAssignedUser.builder()
      .id(row.getUUID(ID_COLUMN))
      .credentialsId(row.getUUID(CREDENTIALS_ID))
      .username(row.getString(USER_NAME))
      .firstName(row.getString(FIRST_NAME))
      .middleName(row.getString(MIDDLE_NAME))
      .lastName(row.getString(LAST_NAME))
      .patronGroup(row.getString(PATRON_GROUP))
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
