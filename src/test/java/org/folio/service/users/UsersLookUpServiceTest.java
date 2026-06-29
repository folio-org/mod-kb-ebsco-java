package org.folio.service.users;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.service.users.UsersLookUpService.USERS_BY_ID_ENDPOINT;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import org.folio.holdingsiq.model.RequestContext;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.util.TestFutureFailedException;
import org.folio.util.WireMockTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UsersLookUpServiceTest extends WireMockTestBase {

  private static Map<String, String> defaultHeaders;
  private static RequestContext requestContext;

  private final UsersLookUpService usersLookUpService = new UsersLookUpService(Vertx.vertx());

  @BeforeAll
  static void beforeAll() {
    defaultHeaders = Map.of(
      XOkapiHeaders.TENANT, STUB_TENANT,
      XOkapiHeaders.USER_ID, USER_1,
      XOkapiHeaders.URL, wm.baseUrl()
    );
    requestContext = new RequestContext(defaultHeaders);
  }

  @Test
  void shouldReturn200WhenThirdPartyUserIdIsValid() {
    var userInfo = result(usersLookUpService.lookUpUserById(USER_1, requestContext));
    assertUserInfo(userInfo);
  }

  @Test
  void shouldReturn200WhenUserIdIsValid() {
    var userInfo = result(usersLookUpService.lookUpUser(requestContext));
    assertUserInfo(userInfo);
  }

  @Test
  void shouldReturn200ByCqlUserIdList() {
    var ids = List.of(UUID.fromString(JANE_ID), UUID.fromString(JOHN_ID));

    var userInfo = result(usersLookUpService.lookUpUsers(ids, requestContext));
    assertNotNull(userInfo);
    assertEquals(2, userInfo.size());
  }

  @Test
  void shouldReturn200WhenGroupIdIsValid() {
    var ids = List.of(UUID.fromString(JOHN_GROUP_ID), UUID.fromString(JANE_GROUP_ID));

    var group = result(usersLookUpService.lookUpGroups(ids, requestContext));
    assertNotNull(group);
    assertEquals(2, group.size());
  }

  @Test
  void shouldReturn401WhenUnauthorizedAccess() {
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(USER_1)), SC_UNAUTHORIZED);

    var lookupFuture = usersLookUpService.lookUpUser(requestContext);
    var ex = assertThrows(TestFutureFailedException.class, () -> result(lookupFuture));
    assertInstanceOf(NotAuthorizedException.class, ex.getCause());
  }

  @Test
  void shouldReturn404WhenUserNotFound() {
    var stubUserId = "xyz";
    var headers = new HashMap<>(defaultHeaders);
    headers.put(XOkapiHeaders.USER_ID, stubUserId);
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(stubUserId)), SC_NOT_FOUND);

    var lookupFuture = usersLookUpService.lookUpUser(new RequestContext(headers));
    var ex = assertThrows(TestFutureFailedException.class, () -> result(lookupFuture));
    assertInstanceOf(NotFoundException.class, ex.getCause());
  }

  @Test
  void shouldReturn404WhenUserNotFoundById() {
    final String stubUserId = "xyz";
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(stubUserId)), SC_NOT_FOUND);

    var lookupFuture = usersLookUpService.lookUpUserById(stubUserId, requestContext);
    var ex = assertThrows(TestFutureFailedException.class, () -> result(lookupFuture));
    assertInstanceOf(NotFoundException.class, ex.getCause());
  }

  private void assertUserInfo(User userInfo) {
    assertNotNull(userInfo);
    assertEquals("cedrick", userInfo.getUserName());
    assertEquals("firstname_test", userInfo.getFirstName());
    assertNull(userInfo.getMiddleName());
    assertEquals("lastname_test", userInfo.getLastName());
  }
}
