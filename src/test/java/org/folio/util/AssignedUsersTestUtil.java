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
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.converter.assignedusers.AssignedUserCollectionItemConverter;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.persist.PostgresClient;

public class AssignedUsersTestUtil {

  public static final String KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT = KB_CREDENTIALS_ENDPOINT + "/%s/users";

  private static final Converter<DbAssignedUser, AssignedUser> CONVERTER =
    new AssignedUserCollectionItemConverter();

  public static void insertAssignedUser(String credentialsId, String username, String firstName, String middleName,
                                        String lastName, String patronGroup, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = String.format(UPSERT_ASSIGNED_USERS_QUERY, kbAssignedUsersTestTable());
    JsonArray params = DbUtils.createParams(Arrays.asList(UUID.randomUUID().toString(), credentialsId, username,
      firstName, middleName, lastName, patronGroup
    ));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();
  }

  public static List<AssignedUser> getAssignedUsers(Vertx vertx) {
    CompletableFuture<List<AssignedUser>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(String.format(SqlQueryHelper.selectQuery(), kbAssignedUsersTestTable()),
      event -> future.complete(event.result().getRows().stream()
        .map(AssignedUsersTestUtil::parseAssignedUser)
        .map(CONVERTER::convert)
        .collect(Collectors.toList())));
    return future.join();
  }

  private static DbAssignedUser parseAssignedUser(JsonObject row) {
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
