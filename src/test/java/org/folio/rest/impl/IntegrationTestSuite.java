package org.folio.rest.impl;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.folio.rest.impl.integrationsuite.DefaultLoadHoldingsImplTest;
import org.folio.rest.impl.integrationsuite.DefaultLoadServiceFacadeTest;
import org.folio.rest.impl.integrationsuite.EHoldingsProxyTypesImplTest;
import org.folio.rest.impl.integrationsuite.EHoldingsRootProxyImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsAccessTypesImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsAssignedUsersImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsCacheImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsConfigurationTest;
import org.folio.rest.impl.integrationsuite.EholdingsCustomLabelsImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsKbCredentialsImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsPackagesTest;
import org.folio.rest.impl.integrationsuite.EholdingsProvidersImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsResourcesImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsStatusTest;
import org.folio.rest.impl.integrationsuite.EholdingsTagsImplTest;
import org.folio.rest.impl.integrationsuite.EholdingsTitlesTest;
import org.folio.rest.impl.integrationsuite.LoadHoldingsStatusImplTest;
import org.folio.rest.impl.integrationsuite.TransactionLoadServiceFacadeTest;
import org.folio.test.util.TestSetUpHelper;

@Suite.SuiteClasses({
  EholdingsCacheImplTest.class,
  EholdingsConfigurationTest.class,
  EholdingsPackagesTest.class,
  EholdingsProvidersImplTest.class,
  EHoldingsProxyTypesImplTest.class,
  EholdingsResourcesImplTest.class,
  EHoldingsRootProxyImplTest.class,
  EholdingsCustomLabelsImplTest.class,
  EholdingsStatusTest.class,
  EholdingsTitlesTest.class,
  EholdingsTagsImplTest.class,
  DefaultLoadHoldingsImplTest.class,
  LoadHoldingsStatusImplTest.class,
  DefaultLoadServiceFacadeTest.class,
  TransactionLoadServiceFacadeTest.class,
  EholdingsAccessTypesImplTest.class,
  EholdingsKbCredentialsImplTest.class,
  EholdingsAssignedUsersImplTest.class
})
@RunWith(Suite.class)
public class IntegrationTestSuite {

  @BeforeClass
  public static void setUpClass() {
    TestSetUpHelper.startVertxAndPostgres(Collections.singletonMap("spring.configuration", "org.folio.spring.config.TestConfig"));
  }

  @AfterClass
  public static void tearDownClass() {
    TestSetUpHelper.stopVertxAndPostgres();
  }
}
