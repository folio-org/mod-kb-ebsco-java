package org.folio.rest.impl;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

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
  TransactionLoadHoldingsImplTest.class,
  TransactionLoadServiceFacadeTest.class
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
