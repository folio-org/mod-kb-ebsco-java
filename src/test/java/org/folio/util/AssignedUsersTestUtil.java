package org.folio.util;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.UPSERT_ASSIGNED_USERS_QUERY;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;

import org.folio.db.DbUtils;
import org.folio.rest.persist.PostgresClient;

public class AssignedUsersTestUtil {

  public static void insertAssignedUsers(String credentialsId, String username, String firstName, String middleName,
                                         String lastName, String patronGroup, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = String.format(UPSERT_ASSIGNED_USERS_QUERY, kbAssignedUsersTestTable());
    JsonArray params = DbUtils.createParams(Arrays.asList(UUID.randomUUID().toString(), credentialsId, username,
      firstName, middleName, lastName, patronGroup
    ));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();
  }

  private static String kbAssignedUsersTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ASSIGNED_USERS_TABLE_NAME;
  }
}
