package org.folio.util;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_VIEW_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.INSERT_ASSIGNED_USER_QUERY;
import static org.folio.repository.users.UsersTableConstants.FIRST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.LAST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.MIDDLE_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.PATRON_GROUP_COLUMN;
import static org.folio.repository.users.UsersTableConstants.USER_NAME_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.db.RowSetUtils;
import org.folio.repository.DbUtil;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.converter.assignedusers.AssignedUserCollectionItemConverter;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.persist.PostgresClient;

public class AssignedUsersTestUtil {

  private static final Converter<DbAssignedUser, AssignedUser> CONVERTER =
    new AssignedUserCollectionItemConverter.FromDb();

  public static String saveAssignedUser(String id, String credentialsId, Vertx vertx) {
    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();

    String insertStatement = DbUtil.prepareQuery(INSERT_ASSIGNED_USER_QUERY, kbAssignedUsersTestTable());
    Tuple params = DbUtils.createParams(toUUID(id), toUUID(credentialsId));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static List<AssignedUser> getAssignedUsers(Vertx vertx) {
    CompletableFuture<List<AssignedUser>> future = new CompletableFuture<>();
    String query = DbUtil.prepareQuery(SqlQueryHelper.selectQuery(), kbAssignedUsersTestView());
    PostgresClient.getInstance(vertx).select(query, event ->
      future.complete(RowSetUtils.mapItems(event.result(), AssignedUsersTestUtil::parseAssignedUser))
    );
    return future.join();
  }

  private static AssignedUser parseAssignedUser(Row row) {
    return CONVERTER.convert(DbAssignedUser.builder()
      .id(row.getUUID(ID_COLUMN))
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .username(row.getString(USER_NAME_COLUMN))
      .patronGroup(row.getString(PATRON_GROUP_COLUMN))
      .firstName(row.getString(FIRST_NAME_COLUMN))
      .middleName(row.getString(MIDDLE_NAME_COLUMN))
      .lastName(row.getString(LAST_NAME_COLUMN))
      .build());
  }

  private static String kbAssignedUsersTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ASSIGNED_USERS_TABLE_NAME;
  }

  private static String kbAssignedUsersTestView() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ASSIGNED_USERS_VIEW_NAME;
  }
}
