package org.folio.util;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.repository.users.UsersTableConstants.FIRST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.LAST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.MIDDLE_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.PATRON_GROUP_COLUMN;
import static org.folio.repository.users.UsersTableConstants.SAVE_USER_QUERY;
import static org.folio.repository.users.UsersTableConstants.USERS_TABLE_NAME;
import static org.folio.repository.users.UsersTableConstants.USER_NAME_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

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
import org.folio.repository.DbUtil;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.users.DbUser;
import org.folio.rest.converter.users.UserConverter;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.users.User;

public class UsersTestUtil {

  private static final Converter<DbUser, User> CONVERTER = new UserConverter.FromDb();

  public static String saveUser(String id, String username, String firstName, String middleName, String lastName,
                                String patronGroup, Vertx vertx) {
    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();

    String insertStatement = DbUtil.prepareQuery(SAVE_USER_QUERY, kbUsersTestTable());
    Tuple params = DbUtils.createParams(toUUID(id), username, firstName, middleName, lastName, patronGroup);

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static String saveUser(String username, String firstName, String middleName,
                                String lastName, String patronGroup, Vertx vertx) {
    return saveUser(fromUUID(UUID.randomUUID()), username, firstName, middleName, lastName, patronGroup, vertx);
  }

  public static List<User> getUsers(Vertx vertx) {
    CompletableFuture<List<User>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(String.format(SqlQueryHelper.selectQuery(), kbUsersTestTable()),
      event -> future.complete(RowSetUtils.mapItems(event.result(), UsersTestUtil::parseUser)));
    return future.join();
  }

  private static User parseUser(Row row) {
    return CONVERTER.convert(DbUser.builder()
      .id(row.getUUID(ID_COLUMN))
      .username(row.getString(USER_NAME_COLUMN))
      .patronGroup(row.getString(PATRON_GROUP_COLUMN))
      .firstName(row.getString(FIRST_NAME_COLUMN))
      .middleName(row.getString(MIDDLE_NAME_COLUMN))
      .lastName(row.getString(LAST_NAME_COLUMN))
      .build());
  }

  private static String kbUsersTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + USERS_TABLE_NAME;
  }
}
