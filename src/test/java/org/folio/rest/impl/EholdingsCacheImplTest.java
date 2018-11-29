package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EholdingsCacheImplTest extends WireMockTestBase {

  @Test
  public void shouldHaveCacheMissWhenDeleteCacheRequestIsSent() throws IOException, URISyntaxException {
    int cacheMissCount = 0;
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    sendGetConfigurationRequest();
    sendGetConfigurationRequest();

    //Verify that second request didn't cause a cache miss, because configuration was cached
    WireMock.verify(++cacheMissCount, getRequestedFor(urlMatching("/configurations/entries.*")));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/cache")
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    sendGetConfigurationRequest();

    WireMock.verify(++cacheMissCount, getRequestedFor(urlMatching("/configurations/entries.*")));
  }

  private void sendGetConfigurationRequest() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(200);
  }
}
