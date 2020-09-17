package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.UCSettingsTestUtil.UC_SETTINGS_ENDPOINT;
import static org.folio.util.UCSettingsTestUtil.saveUCSettings;
import static org.folio.util.UCSettingsTestUtil.stubSettings;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.util.AssertTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsKbCredentialsUCImplTest extends WireMockTestBase {

  private String credentialsId;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnUCSettingsOnGet() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUCSettings(stubSettings, vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    UCSettings actual = getWithOk(resourcePath).as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn404OnGetNotExistedSettings() {
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    JsonapiError actual = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    String expectedErrorMessage = String.format("Usage Consolidation is not "
      + "enabled for KB credentials with id [%s]", credentialsId);
    AssertTestUtil.assertErrorContainsTitle(actual, expectedErrorMessage);
  }
}
