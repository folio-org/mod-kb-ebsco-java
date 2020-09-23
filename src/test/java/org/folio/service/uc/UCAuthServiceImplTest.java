package org.folio.service.uc;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;

import java.lang.reflect.Method;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.client.uc.UCAuthServiceClient;
import org.folio.client.uc.UCAuthToken;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;

@RunWith(VertxUnitRunner.class)
public class UCAuthServiceImplTest extends WireMockTestBase {

  public static final int TIMEOUT = 100000;
  @Autowired
  private UCAuthService ucAuthService;
  @InjectMocks
  @Autowired
  private UCAuthServiceClient authServiceClient;
  @Spy
  private HttpResponse<JsonObject> httpResponse;
  @Spy
  private HttpRequest<JsonObject> httpRequest;

  private static <T> HandlerAnswer<AsyncResult<HttpResponse<T>>, Void> httpResponseAnswer(HttpResponse<T> httpResponse,
                                                                                          int argumentIndex) {
    AsyncResult<HttpResponse<T>> res = succeededFuture(httpResponse);
    return new HandlerAnswer<>(res, argumentIndex);
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    openMocks(this).close();
    when(httpRequest.expect(any())).then(invocation -> {
      Method method = invocation.getMethod();
      return httpRequest;
    });
    doAnswer(httpResponseAnswer(httpResponse, 1)).when(httpRequest).sendForm(any(), any());
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
