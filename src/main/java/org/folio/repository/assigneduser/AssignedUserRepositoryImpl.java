package org.folio.repository.assigneduser;

import static java.lang.String.format;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getAssignedUsersTableName;
import static org.folio.repository.assigneduser.AssignedUsersConstants.CREDENTIALS_ID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.FIRST_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.LAST_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.MIDDLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.PATRON_GROUP;
import static org.folio.repository.assigneduser.AssignedUsersConstants.SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.USER_NAME;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.persist.PostgresClient;

@Component
public class AssignedUserRepositoryImpl implements AssignedUserRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AssignedUserRepositoryImpl.class);
  private static final String SELECT_LOG_MESSAGE = "Do select query = {}";

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
