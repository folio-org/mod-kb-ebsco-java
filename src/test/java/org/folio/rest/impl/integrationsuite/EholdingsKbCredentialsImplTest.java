package org.folio.rest.impl.integrationsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_USERNAME;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.util.KbCredentialsTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsKbCredentialsImplTest extends WireMockTestBase {

  @After
  public void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnKbCredentialsCollectionOnGet() {
    KbCredentialsTestUtil.insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    KbCredentialsCollection actual = getWithOk(KB_CREDENTIALS_ENDPOINT).as(KbCredentialsCollection.class);

    assertEquals(1, actual.getData().size());
    assertEquals(Integer.valueOf(1), actual.getMeta().getTotalResults());
    assertNotNull(actual.getData().get(0));
    assertNotNull(actual.getData().get(0).getId());
    assertEquals(STUB_API_URL, actual.getData().get(0).getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME, actual.getData().get(0).getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, actual.getData().get(0).getAttributes().getCustomerId());
    assertEquals(STUB_USERNAME, actual.getData().get(0).getMeta().getCreatedByUsername());
    assertEquals(StringUtils.repeat("*", 40), actual.getData().get(0).getAttributes().getApiKey());
  }

  @Test
  public void shouldReturnEmptyKbCredentialsCollectionOnGet() {
    KbCredentialsCollection actual = getWithOk(KB_CREDENTIALS_ENDPOINT).as(KbCredentialsCollection.class);

    assertNotNull(actual.getData());
    assertEquals(0, actual.getData().size());
    assertEquals(Integer.valueOf(0), actual.getMeta().getTotalResults());
  }
}
