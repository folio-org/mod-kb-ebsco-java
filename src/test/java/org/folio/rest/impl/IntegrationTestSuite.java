package org.folio.rest.impl;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@Suite.SuiteClasses({
  EholdingsCacheImplTest.class,
  EholdingsConfigurationTest.class,
  EholdingsPackagesTest.class,
  EholdingsProvidersImplTest.class,
  EHoldingsProxyTypesImplTest.class,
  EholdingsResourcesImplTest.class,
  EHoldingsRootProxyImplTest.class,
  EholdingsStatusTest.class,
  EholdingsTitlesTest.class,
  EholdingsTagsImplTest.class
})
@RunWith(Suite.class)
public class IntegrationTestSuite {
  @BeforeClass
  public static void setUpClass() throws IOException {
    TestSetUpHelper.startVertxAndPostgres();
  }

  @AfterClass
  public static void tearDownClass() {
    TestSetUpHelper.stopVertxAndPostgres();
  }
}
