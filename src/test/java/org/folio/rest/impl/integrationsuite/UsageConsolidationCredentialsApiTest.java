package org.folio.rest.impl.integrationsuite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.folio.util.UCCredentialsTestUtil.UC_CREDENTIALS_ENDPOINT;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;

@RunWith(VertxUnitRunner.class)
public class UsageConsolidationCredentialsApiTest extends WireMockTestBase {

  @Test
  public void shouldReturnUCCredentialsPresenceWithTrueOnGetWhenCredentialsExists() {
    setUpUCCredentials(vertx);

    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertTrue(actual.getAttributes().getIsPresent());
  }

  @Test
  public void shouldReturnUCCredentialsPresenceWithFalseOnGetWhenCredentialsNotExist() {
    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertFalse(actual.getAttributes().getIsPresent());
  }
}