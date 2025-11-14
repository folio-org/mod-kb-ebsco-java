package org.folio.service.uc;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import java.util.Map;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.client.uc.UcAuthEbscoClient;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(VertxUnitRunner.class)
public class UcAuthServiceImplTest extends WireMockTestBase {

  public static final int TIMEOUT = 100000;
  @Autowired
  private UcAuthService ucAuthService;
  @Autowired
  private UcAuthEbscoClient authServiceClient;
  @Spy
  private WebClient client;
  @Spy
  private HttpResponse<JsonObject> httpResponse;
  @Spy
  private HttpRequest<Buffer> httpRequest;
  @Spy
  private HttpRequest<JsonObject> jsonHttpRequest;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    openMocks(this).close();
    ReflectionTestUtils.setField(authServiceClient, "webClient", client);

    when(client.postAbs(anyString())).thenReturn(httpRequest);
    when(jsonHttpRequest.sendForm(any())).thenReturn(succeededFuture(httpResponse));
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.as(BodyCodec.jsonObject())).thenReturn(jsonHttpRequest);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void returnTokenWhenCredentialsValid(TestContext context) {
    UcAuthToken mockedToken = new UcAuthToken("access-token", "token-type", 1000L, "scope");

    when(httpResponse.statusCode()).thenReturn(SC_OK);
    when(httpResponse.body()).thenReturn(JsonObject.mapFrom(mockedToken));

    setUpUcCredentials(vertx);
    Async async = context.async();
    ucAuthService.authenticate(new CaseInsensitiveMap<>(Map.of(XOkapiHeaders.TENANT, STUB_TENANT)))
      .thenAccept(expected -> {
        context.assertEquals(expected, mockedToken.accessToken());
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
}
