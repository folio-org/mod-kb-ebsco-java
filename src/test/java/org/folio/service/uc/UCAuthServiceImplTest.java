package org.folio.service.uc;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import static org.folio.repository.uc.UCCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;

import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;

@RunWith(VertxUnitRunner.class)
public class UCAuthServiceImplTest extends WireMockTestBase {

  public static final int TIMEOUT = 100000;
  @Autowired
  private UCAuthService ucAuthService;
  @Autowired
  private UCAuthEbscoClient authServiceClient;
  @Spy
  private WebClient client;
  @Spy
  private HttpResponse<JsonObject> httpResponse;
  @Spy
  private HttpRequest<Buffer> httpRequest;
  @Spy
  private HttpRequest<JsonObject> jsonHttpRequest;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    openMocks(this).close();
    ReflectionTestUtils.setField(authServiceClient, "webClient", client);

    when(client.postAbs(anyString())).thenReturn(httpRequest);
    doAnswer(httpResponseAnswer(httpResponse, 1)).when(jsonHttpRequest).sendForm(any(), any(Handler.class));
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.expect(any(ResponsePredicate.class))).thenReturn(httpRequest);
    when(httpRequest.as(BodyCodec.jsonObject())).thenReturn(jsonHttpRequest);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void returnTokenWhenCredentialsValid(TestContext context) {
    UCAuthToken mockedToken = new UCAuthToken("access-token", "token-type", 1000L, "scope");

    when(httpResponse.statusCode()).thenReturn(SC_OK);
    when(httpResponse.body()).thenReturn(JsonObject.mapFrom(mockedToken));

    setUpUCCredentials(vertx);
    Async async = context.async();
    ucAuthService.authenticate(new CaseInsensitiveMap<>(Map.of(XOkapiHeaders.TENANT, STUB_TENANT)))
      .thenAccept(expected -> {
        context.assertEquals(expected, mockedToken.getAccessToken());
        async.complete();
      })
      .exceptionally(throwable -> {
        context.fail(throwable);
        async.complete();
        return null;
      });
    async.await(TIMEOUT);
  }

  @Test
  public void returnTokenWhenCredentialsAreNotExist(TestContext context) {
    Async async = context.async();
    ucAuthService.authenticate(new CaseInsensitiveMap<>(Map.of(XOkapiHeaders.TENANT, STUB_TENANT)))
      .exceptionally(throwable -> {
        context.assertEquals(UcAuthenticationException.class, throwable.getCause().getClass());
        async.complete();
        return null;
      });
    async.await(TIMEOUT);
  }

  private static <T> HandlerAnswer<AsyncResult<HttpResponse<T>>, Void> httpResponseAnswer(HttpResponse<T> httpResponse,
                                                                                          int argumentIndex) {
    AsyncResult<HttpResponse<T>> res = succeededFuture(httpResponse);
    return new HandlerAnswer<>(res, argumentIndex);
  }

  private static class HandlerAnswer<H, R> implements Answer<R> {

    private final H handlerResult;
    private final int argumentIndex;
    private R returnResult;

    public HandlerAnswer(H handlerResult, int handlerArgumentIndex) {
      this.handlerResult = handlerResult;
      this.argumentIndex = handlerArgumentIndex;
    }

    public HandlerAnswer(H handlerResult, int handlerArgumentIndex, R returnResult) {
      this(handlerResult, handlerArgumentIndex);
      this.returnResult = returnResult;
    }

    @Override
    public R answer(InvocationOnMock invocation) {
      Handler<H> handler = invocation.getArgument(argumentIndex);
      handler.handle(handlerResult);
      return returnResult;
    }
  }
}
