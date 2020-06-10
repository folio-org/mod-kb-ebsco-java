package org.folio.repository.assigneduser;

import static java.lang.String.format;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.foreignKeyConstraintRecover;
import static org.folio.repository.DbUtil.getAssignedUsersTableName;
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
import static org.folio.repository.assigneduser.AssignedUsersConstants.UPDATE_ASSIGNED_USER_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.USER_NAME;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.exc.ServiceExceptions;

@Component
public class AssignedUserRepositoryImpl implements AssignedUserRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AssignedUserRepositoryImpl.class);

  private static final String SELECT_LOG_MESSAGE = "Do select query = {}";
  private static final String INSERT_LOG_MESSAGE = "Do insert query = {}";
  private static final String UPDATE_LOG_MESSAGE = "Do update query = {}";
  private static final String DELETE_LOG_MESSAGE = "Do delete query = {}";

  private static final String USER_ASSIGN_NOT_ALLOWED_MESSAGE = "The user is already assigned to another credentials";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Collection<DbAssignedUser>> findByCredentialsId(String credentialsId, String tenant) {
    String query = format(SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY, getAssignedUsersTableName(tenant));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenant).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapAssignedUserCollection);
  }

  @Override
  public CompletableFuture<DbAssignedUser> save(DbAssignedUser entity, String tenant) {
    String query = format(INSERT_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    JsonArray params = createParams(Arrays.asList(
      entity.getId(),
      entity.getCredentialsId(),
      entity.getUsername(),
      entity.getFirstName(),
      entity.getMiddleName(),
      entity.getLastName(),
      entity.getPatronGroup()
    ));

    LOG.info(INSERT_LOG_MESSAGE, query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<UpdateResult> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(pkConstraintRecover(ID_COLUMN, new BadRequestException(USER_ASSIGN_NOT_ALLOWED_MESSAGE)))
      .recover(foreignKeyConstraintRecover(ServiceExceptions.notFound(KbCredentials.class, entity.getCredentialsId())));
    return mapResult(resultFuture, updateResult -> entity);
  }

  @Override
  public CompletableFuture<Void> update(DbAssignedUser dbAssignedUser, String tenant) {
    String query = format(UPDATE_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    JsonArray params = createParams(Arrays.asList(
      dbAssignedUser.getUsername(),
      dbAssignedUser.getFirstName(),
      dbAssignedUser.getMiddleName(),
      dbAssignedUser.getLastName(),
      dbAssignedUser.getPatronGroup(),
      dbAssignedUser.getId(),
      dbAssignedUser.getCredentialsId()
    ));

    LOG.info(UPDATE_LOG_MESSAGE, query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<UpdateResult> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy());
    return mapResult(resultFuture, updateResult -> checkUserFound(dbAssignedUser.getId(), updateResult));
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, String tenant) {
    String query = format(DELETE_ASSIGNED_USER_QUERY, getAssignedUsersTableName(tenant));

    LOG.info(DELETE_LOG_MESSAGE, query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenant).execute(query, createParams(Arrays.asList(credentialsId, userId)), promise);

    Future<UpdateResult> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy());
    return mapResult(resultFuture, updateResult -> checkUserFound(userId, updateResult));
  }

  @Nullable
  private Void checkUserFound(String userId, UpdateResult updateResult) {
    if (updateResult.getUpdated() == 0) {
      throw ServiceExceptions.notFound("Assigned User", userId);
    }
    return null;
  }

  private Collection<DbAssignedUser> mapAssignedUserCollection(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapAssignedUserItem);
  }

  private DbAssignedUser mapAssignedUserItem(JsonObject row) {
    return DbAssignedUser.builder()
      .id(row.getString(ID_COLUMN))
      .credentialsId(row.getString(CREDENTIALS_ID))
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
