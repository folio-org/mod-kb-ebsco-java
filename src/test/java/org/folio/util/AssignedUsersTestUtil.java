package org.folio.util;

import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;

import org.folio.db.DbUtils;
import org.folio.repository.SqlQueryHelper;
import org.folio.rest.persist.PostgresClient;

public class AssignedUsersTestUtil {

  public static final String ASSIGNED_USERS_TABLE_NAME = "assigned_users";

  private static final String[] TABLE_COLUMNS = {
    "id",
    "credentials_id",
    "username",
    "patron_group",
    "first_name",
    "middle_name",
    "last_name"
  };

  public static void insertAssignedUsers(String credentialsId, String username, String patronGroup,
                                         String firstName, String middleName, String lastName, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = String.format(SqlQueryHelper.insertQuery(TABLE_COLUMNS), kbAssignedUsersTestTable());
    JsonArray params = DbUtils.createParams(Arrays.asList(UUID.randomUUID().toString(), credentialsId, username, patronGroup,
      firstName, middleName, lastName
    ));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();
  }

  private static String kbAssignedUsersTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ASSIGNED_USERS_TABLE_NAME;
  }
}
