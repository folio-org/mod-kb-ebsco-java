package org.folio.repository.assigneduser;

import static org.folio.common.LogUtils.logCountQuery;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.getAssignedUsersTableName;
import static org.folio.repository.DbUtil.getAssignedUsersViewName;
import static org.folio.repository.DbUtil.pkConstraintRecover;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.assigneduser.AssignedUsersConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.DELETE_ASSIGNED_USER_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.INSERT_ASSIGNED_USER_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.users.UsersTableConstants.FIRST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.LAST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.MIDDLE_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.PATRON_GROUP_COLUMN;
import static org.folio.repository.users.UsersTableConstants.USER_NAME_COLUMN;
import static org.folio.service.exc.ServiceExceptions.notFound;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Arrays;
import java.util.Collection;
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
import org.folio.service.users.User;

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
    String query = prepareQuery(SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY, getAssignedUsersViewName(tenant));
    Tuple params = createParams(credentialsId);

    logSelectQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAssignedUserCollection);
  }

  @Override
  public CompletableFuture<Integer> count(UUID credentialsId, String tenant) {
    String query = prepareQuery(SELECT_COUNT_BY_CREDENTIALS_ID_QUERY, getAssignedUsersTableName(tenant));
    Tuple params = Tuple.of(credentialsId);

    logCountQuery(LOG, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()),
      rs -> mapFirstItem(rs, row -> row.getInteger(0)));
  }

  @Override
  public CompletableFuture<DbAssignedUser> save(DbAssignedUser entity, String tenant) {
    String query = prepareQuery(INSERT_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    Tuple params = createParams(Arrays.asList(
      entity.getId(),
      entity.getCredentialsId()
    ));

    logInsertQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<RowSet<Row>> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(pkConstraintRecover(ID_COLUMN, new BadRequestException(USER_ASSIGN_NOT_ALLOWED_MESSAGE)))
      .recover(foreignKeyConstraintRecover(notFound(KbCredentials.class, entity.getCredentialsId().toString())))
      .recover(foreignKeyConstraintRecover(notFound(User.class, entity.getId().toString())));
    return mapResult(resultFuture, updateResult -> entity);
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, UUID userId, String tenant) {
    String query = prepareQuery(DELETE_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));
    Tuple params = createParams(credentialsId, userId);

    logDeleteQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

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
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .username(row.getString(USER_NAME_COLUMN))
      .firstName(row.getString(FIRST_NAME_COLUMN))
      .middleName(row.getString(MIDDLE_NAME_COLUMN))
      .lastName(row.getString(LAST_NAME_COLUMN))
      .patronGroup(row.getString(PATRON_GROUP_COLUMN))
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
