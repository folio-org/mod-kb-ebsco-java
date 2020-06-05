package org.folio.util;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.CREDENTIALS_ID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.FIRST_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.repository.assigneduser.AssignedUsersConstants.LAST_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.MIDDLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.PATRON_GROUP;
import static org.folio.repository.assigneduser.AssignedUsersConstants.UPSERT_ASSIGNED_USERS_QUERY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.USER_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.db.RowSetUtils;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.converter.assignedusers.AssignedUserCollectionItemConverter;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.persist.PostgresClient;

public class AssignedUsersTestUtil {

  private static final Converter<DbAssignedUser, AssignedUser> CONVERTER =
    new AssignedUserCollectionItemConverter.FromDb();

  public static String insertAssignedUser(String id, String credentialsId, String username, String firstName,
                                          String middleName, String lastName, String patronGroup, Vertx vertx) {
    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();

    String insertStatement = String.format(UPSERT_ASSIGNED_USERS_QUERY, kbAssignedUsersTestTable());
    Tuple params = DbUtils.createParams(Arrays.asList(id, credentialsId, username,
      firstName, middleName, lastName, patronGroup
    ));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static String insertAssignedUser(String credentialsId, String username, String firstName, String middleName,
                                          String lastName, String patronGroup, Vertx vertx) {
    return insertAssignedUser(UUID.randomUUID().toString(), credentialsId, username, firstName, middleName, lastName,
      patronGroup, vertx);
  }

  public static List<AssignedUser> getAssignedUsers(Vertx vertx) {
    CompletableFuture<List<AssignedUser>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(String.format(SqlQueryHelper.selectQuery(), kbAssignedUsersTestTable()),
      event -> future.complete(RowSetUtils.mapItems(event.result(), row -> CONVERTER.convert(parseAssignedUser(row)))));
    return future.join();
  }

  private static DbAssignedUser parseAssignedUser(Row row) {
    return DbAssignedUser.builder()
      .id(row.getString(ID_COLUMN))
      .credentialsId(row.getString(CREDENTIALS_ID))
      .username(row.getString(USER_NAME))
      .patronGroup(row.getString(PATRON_GROUP))
      .firstName(row.getString(FIRST_NAME))
      .middleName(row.getString(MIDDLE_NAME))
      .lastName(row.getString(LAST_NAME))
      .build();
  }

  private static String kbAssignedUsersTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ASSIGNED_USERS_TABLE_NAME;
  }
}
