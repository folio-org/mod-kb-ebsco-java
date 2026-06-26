package org.folio.service.uc;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.result;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import java.util.Map;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.client.uc.UcAuthEbscoClient;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.util.IntegrationTestBase;
import org.folio.util.TestFutureFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UcAuthServiceImplIntegrationTest extends IntegrationTestBase {

  private static final CaseInsensitiveMap<String, String> HEADERS =
    new CaseInsensitiveMap<>(Map.of(XOkapiHeaders.TENANT, STUB_TENANT));

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

  @BeforeEach
  void beforeEach() {
    ReflectionTestUtils.setField(authServiceClient, "webClient", client);

    when(client.postAbs(anyString())).thenReturn(httpRequest);
    when(jsonHttpRequest.sendForm(any())).thenReturn(succeededFuture(httpResponse));
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.as(BodyCodec.jsonObject())).thenReturn(jsonHttpRequest);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void returnTokenWhenCredentialsValid() {
    var mockedToken = new UcAuthToken("access-token", "token-type", 1000L, "scope");

    when(httpResponse.statusCode()).thenReturn(SC_OK);
    when(httpResponse.body()).thenReturn(JsonObject.mapFrom(mockedToken));

    setUpUcCredentials(vertx);
    var result = result(ucAuthService.authenticate(HEADERS));

    assertEquals(mockedToken.accessToken(), result);
  }

  @Test
  void returnTokenWhenCredentialsAreNotExist() {
    var authResult = ucAuthService.authenticate(HEADERS);
    var ex = assertThrows(TestFutureFailedException.class, () -> result(authResult));

    assertEquals(UcAuthenticationException.class, ex.getCause().getClass());
  }
}
