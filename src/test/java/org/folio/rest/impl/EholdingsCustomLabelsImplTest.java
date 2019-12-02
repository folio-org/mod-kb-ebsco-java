package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.apache.http.HttpStatus.SC_NOT_IMPLEMENTED;
import static org.junit.Assert.assertEquals;

import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(VertxUnitRunner.class)
public class EholdingsCustomLabelsImplTest extends WireMockTestBase {

  private static final String CUSTOM_LABELS_PATH = "/eholdings/custom-labels";

  @Test
  public void shouldReturnCustomLabelsOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String labels = getWithOk(CUSTOM_LABELS_PATH).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/custom-labels/get-custom-labels-list.json"),
      labels, false);
  }

  @Test
  public void shouldReturn501WhenGetByIdIsNotImplemented(){
    final int expectedCode = SC_NOT_IMPLEMENTED;
    final int code = getWithStatus(CUSTOM_LABELS_PATH + "/1", expectedCode).response().statusCode();
    assertEquals(expectedCode, code);
  }

  @Test
  public void shouldReturn501WhenPutByIdIsNotImplemented() throws IOException, URISyntaxException {
    final int expectedCode = SC_NOT_IMPLEMENTED;
    final int code =  putWithStatus(CUSTOM_LABELS_PATH + "/1",
      readFile("requests/kb-ebsco/custom-labels/put-custom-label.json"),expectedCode).response().statusCode();
    assertEquals(expectedCode, code);
  }

  @Test
  public void shouldReturn501WhenDeleteByIdIsNotImplemented(){
    final int expectedCode = SC_NOT_IMPLEMENTED;
    final int code =  deleteWithStatus(CUSTOM_LABELS_PATH + "/1", expectedCode).response().statusCode();
    assertEquals(expectedCode, code);
  }
}
