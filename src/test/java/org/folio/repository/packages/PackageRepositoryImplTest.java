package org.folio.repository.packages;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.spring.config.TestConfig;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class PackageRepositoryImplTest {

  @Autowired
  PackageRepository repository;

  @Test
  public void shouldReturnEmptyListWhenIdListIsEmpty() {
    List<PackageInfoInDB> packages = repository.findByIds(Collections.emptyList(), null, null).join();
    assertThat(packages, empty());
  }
  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    List<PackageInfoInDB> packages = repository.findByTagName(Collections.emptyList(), 1, 25, null, STUB_TENANT).join();
    assertThat(packages, empty());
  }

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmptyAndProviderIdIsPresent() {
    List<PackageInfoInDB> packages = repository.findByTagNameAndProvider(Collections.emptyList(),STUB_VENDOR_ID,1, 25, null, null).join();
    assertThat(packages, empty());
  }
}
