package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ID_COLUMN;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID_2;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.test.util.TestUtil;
import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.db.RowSetUtils;
import org.folio.repository.DbUtil;
import org.folio.repository.SqlQueryHelper;
import org.folio.rest.converter.users.UserConverter;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.users.User;

public class UsersTestUtil {

  private static final String GET_USER_ENDPOINT = "/users/";

  private static final String USER_INFO_STUB_FILE = "responses/userlookup/mock_user_response_200.json";

//  public static void mockGetUser() throws IOException, URISyntaxException {
//    final String stubUserId = "88888888-8888-4888-8888-888888888888";
//    final String stubUserIdEndpoint = GET_USER_ENDPOINT + stubUserId;
//
//    stubFor(
//      get(new UrlPathPattern(new RegexPattern(stubUserIdEndpoint), true))
//        .willReturn(new ResponseDefinitionBuilder()
//          .withBody(TestUtil.readFile(USER_INFO_STUB_FILE))));
//
//  }

//  public static String saveUser(String id, String username, String firstName, String middleName, String lastName,
//                                String patronGroup, Vertx vertx) {
//    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
//
//    String insertStatement = DbUtil.prepareQuery(saveUserQuery(), kbUsersTestTable());
//    Tuple params = DbUtils.createParams(toUUID(id), username, firstName, middleName, lastName, patronGroup);
//
//    PostgresClient.getInstance(vertx, STUB_TENANT).execute(insertStatement, params, event -> future.complete(null));
//    future.join();
//
//    return id;
//  }

//  public static String saveUser(String username, String firstName, String middleName,
//                                String lastName, String patronGroup, Vertx vertx) {
//    return null;
//    return saveUser(fromUUID(UUID.randomUUID()), username, firstName, middleName, lastName, patronGroup, vertx);
//  }

//  public static List<User> getUsers(Vertx vertx) {
//    CompletableFuture<List<User>> future = new CompletableFuture<>();
//    PostgresClient.getInstance(vertx, STUB_TENANT).select(String.format(SqlQueryHelper.selectQuery(), kbUsersTestTable()),
//      event -> future.complete(RowSetUtils.mapItems(event.result(), UsersTestUtil::parseUser)));
//    return future.join();
//  }

//  private static User parseUser(Row row) {
//    return CONVERTER.convert(DbUser.builder()
//      .id(row.getUUID(ID_COLUMN))
//      .username(row.getString(USER_NAME_COLUMN))
//      .patronGroup(row.getString(PATRON_GROUP_COLUMN))
//      .firstName(row.getString(FIRST_NAME_COLUMN))
//      .middleName(row.getString(MIDDLE_NAME_COLUMN))
//      .lastName(row.getString(LAST_NAME_COLUMN))
//      .build());
//    return null;
//  }

//  private static String kbUsersTestTable() {
//    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + USERS_TABLE_NAME;null;
//  }
}
