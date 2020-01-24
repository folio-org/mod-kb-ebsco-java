package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.matching.RegexPattern;

import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.impl.WireMockTestBase;

@RunWith(VertxUnitRunner.class)
public class EHoldingsProxyTypesImplTest extends WireMockTestBase {

  private static final String RMI_PROXIES_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/proxies.*";
  private static final String EHOLDINGS_PROXY_TYPES_URL = "eholdings/proxy-types";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    mockDefaultConfiguration(getWiremockUrl());
  }

  @Test
  public void shouldReturnProxyTypesFromValidRMApiResponse() throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnEmptyProxyTypesFromEmptyRMApiResponse() throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-empty-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-empty-response.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnForbiddenWhenRMAPIRequestCompletesWith401ErrorStatus() {
    mockGet(new RegexPattern(RMI_PROXIES_URL), SC_UNAUTHORIZED);

    getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_FORBIDDEN);
  }

  @Test
  public void shouldReturnForbiddenWhenRMAPIRequestCompletesWith403ErrorStatus() {
    mockGet(new RegexPattern(RMI_PROXIES_URL), SC_FORBIDDEN);

    getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_FORBIDDEN);
  }

}

