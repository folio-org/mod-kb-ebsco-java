package org.folio.rest.impl;

import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(VertxUnitRunner.class)
public class EHoldingsProxyTypesImplTest extends WireMockTestBase {

  private static final String RMI_PROXIES_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/proxies.*";
  private static final String EHOLDINGS_PROXY_TYPES_URL = "eholdings/proxy-types";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    String configurationStubFile = "responses/kb-ebsco/configuration/get-configuration.json";
    mockConfiguration(configurationStubFile, getWiremockUrl());
  }

  @Test
  public void shouldReturnProxyTypesFromValidRMApiResponse() throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String actual = getResponseWithStatus(EHOLDINGS_PROXY_TYPES_URL, HttpStatus.SC_OK).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnEmptyProxyTypesFromEmptyRMApiResponse() throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-empty-response.json");

    String actual = getResponseWithStatus(EHOLDINGS_PROXY_TYPES_URL, HttpStatus.SC_OK).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-empty-response.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnForbiddenWhenRMAPIRequestCompletesWith401ErrorStatus() {
    mockGet(new RegexPattern(RMI_PROXIES_URL), HttpStatus.SC_UNAUTHORIZED);

    getResponse(EHOLDINGS_PROXY_TYPES_URL).statusCode(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  public void shouldReturnForbiddenWhenRMAPIRequestCompletesWith403ErrorStatus() {
    mockGet(new RegexPattern(RMI_PROXIES_URL), HttpStatus.SC_FORBIDDEN);

    getResponse(EHOLDINGS_PROXY_TYPES_URL).statusCode(HttpStatus.SC_FORBIDDEN);
  }

}

