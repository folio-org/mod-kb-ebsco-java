package org.folio.util;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.*;
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

    String insertStatement = DbUtil.prepareQuery(insertAssignedUserQuery(), kbAssignedUsersTestTable());
    Tuple params = DbUtils.createParams(toUUID(id), toUUID(credentialsId));

    PostgresClient.getInstance(vertx, STUB_TENANT).execute(insertStatement, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static List<AssignedUser> getAssignedUsers(Vertx vertx) {
    CompletableFuture<List<AssignedUser>> future = new CompletableFuture<>();
    String query = DbUtil.prepareQuery(SqlQueryHelper.selectQuery(), kbAssignedUsersTestTable());
    PostgresClient.getInstance(vertx, STUB_TENANT).select(query, event ->
      future.complete(RowSetUtils.mapItems(event.result(), AssignedUsersTestUtil::parseAssignedUser))
    );
    return future.join();
  }

  private static AssignedUser parseAssignedUser(Row row) {
    return CONVERTER.convert(DbAssignedUser.builder()
      .id(row.getUUID(ID_COLUMN))
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .build());
  }

  private static String kbAssignedUsersTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ASSIGNED_USERS_TABLE_NAME;
  }
}
