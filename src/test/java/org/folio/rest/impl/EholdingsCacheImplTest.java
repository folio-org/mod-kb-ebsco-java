package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import static org.folio.util.KBTestUtil.mockDefaultConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EholdingsCacheImplTest extends WireMockTestBase {

  @Test
  public void shouldHaveCacheMissWhenDeleteCacheRequestIsSent() throws IOException, URISyntaxException {
    int cacheMissCount = 0;
    mockDefaultConfiguration(getWiremockUrl());

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
    getWithOk("eholdings/configuration");
  }
}
