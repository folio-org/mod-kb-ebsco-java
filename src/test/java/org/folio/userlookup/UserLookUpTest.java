package org.folio.userlookup;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import static org.folio.rest.util.RestConstants.OKAPI_TENANT_HEADER;
import static org.folio.rest.util.RestConstants.OKAPI_TOKEN_HEADER;
import static org.folio.rest.util.RestConstants.OKAPI_URL_HEADER;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import org.folio.rest.util.TokenUtil;
import org.folio.service.userlookup.UserLookUp;
import org.folio.service.userlookup.UserLookUpService;
import org.folio.test.junit.TestStartLoggingRule;
import org.folio.test.util.TestUtil;

@RunWith(VertxUnitRunner.class)
public class UserLookUpTest {

  private static final String HOST = "http://127.0.0.1";
  private static final String USER_INFO_STUB_FILE = "responses/userlookup/mock_user_response_200.json";

  private static final Map<String, String> OKAPI_HEADERS = new HashMap<>();

  private final String GET_USER_ENDPOINT = "/users/";
  private final UserLookUpService userLookUpService = new UserLookUpService();

  @Rule
  public TestRule watcher = TestStartLoggingRule.instance();
  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  @Test
  public void shouldReturn200WhenUserIdIsValid(TestContext context) throws IOException, URISyntaxException {
    final String stubUserId = "88888888-8888-4888-8888-888888888888";
    final String stubToken = TokenUtil.generateToken("cedrick", stubUserId);
    final String stubUserIdEndpoint = GET_USER_ENDPOINT + stubUserId;
    Async async = context.async();

    OKAPI_HEADERS.put(OKAPI_TENANT_HEADER, STUB_TENANT);
    OKAPI_HEADERS.put(OKAPI_URL_HEADER, getWiremockUrl());
    OKAPI_HEADERS.put(OKAPI_TOKEN_HEADER, stubToken);

    stubFor(
      get(new UrlPathPattern(new RegexPattern(stubUserIdEndpoint), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(USER_INFO_STUB_FILE))));

    CompletableFuture<UserLookUp> info = userLookUpService.getUserInfo(OKAPI_HEADERS);
    info.thenCompose(userInfo -> {
      context.assertNotNull(userInfo);

      context.assertEquals("cedrick", userInfo.getUsername());
      context.assertEquals("firstname_test", userInfo.getFirstName());
      context.assertNull(userInfo.getMiddleName());
      context.assertEquals("lastname_test", userInfo.getLastName());

      async.complete();

      return null;
    }).exceptionally(throwable -> {
      context.fail(throwable);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturn401WhenUnauthorizedAccess(TestContext context) {
    final String stubUserId = "a49cefad-7447-4f2f-9004-de32e7a6cc53";
    final String stubToken = TokenUtil.generateToken("cedrick", stubUserId);
    final String stubUserIdEndpoint = GET_USER_ENDPOINT + stubUserId;
    Async async = context.async();

    OKAPI_HEADERS.put(OKAPI_URL_HEADER, getWiremockUrl());
    OKAPI_HEADERS.put(OKAPI_TOKEN_HEADER, stubToken);

    stubFor(
      get(new UrlPathPattern(new RegexPattern(stubUserIdEndpoint), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(401)
          .withStatusMessage("Authorization Failure")));

    CompletableFuture<UserLookUp> info = userLookUpService.getUserInfo(OKAPI_HEADERS);
    info.thenCompose(result -> {
      context.assertNull(result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertTrue(exception.getCause() instanceof NotAuthorizedException);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturn404WhenUserNotFound(TestContext context) {
    final String stubUserId = "xyz";
    final String stubToken = TokenUtil.generateToken("cedrick", stubUserId);
    final String stubUserIdEndpoint = GET_USER_ENDPOINT + stubUserId;
    Async async = context.async();

    OKAPI_HEADERS.put(OKAPI_TENANT_HEADER, STUB_TENANT);
    OKAPI_HEADERS.put(OKAPI_URL_HEADER, getWiremockUrl());
    OKAPI_HEADERS.put(OKAPI_TOKEN_HEADER, stubToken);

    stubFor(
      get(new UrlPathPattern(new RegexPattern(stubUserIdEndpoint), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(404)
          .withStatusMessage("User Not Found")));

    CompletableFuture<UserLookUp> info = userLookUpService.getUserInfo(OKAPI_HEADERS);
    info.thenCompose(result -> {
      context.assertNull(result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertTrue(exception.getCause() instanceof NotFoundException);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturn500WhenMissingOkapiURLHeader(TestContext context) {
    Async async = context.async();

    OKAPI_HEADERS.put(OKAPI_TENANT_HEADER, STUB_TENANT);

    CompletableFuture<UserLookUp> info = userLookUpService.getUserInfo(OKAPI_HEADERS);
    info.thenCompose(result -> {
      context.assertNull(result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertTrue(exception.getCause() instanceof IllegalStateException);
      async.complete();
      return null;
    });
  }

  private String getWiremockUrl() {
    return HOST + ":" + userMockServer.port();
  }
}
