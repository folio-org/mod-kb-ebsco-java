package org.folio.repository.packages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.repository.RecordType;
import org.folio.rest.model.filter.TagFilter;
import org.folio.spring.config.TestConfig;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class PackageRepositoryImplTest {

  @Autowired
  PackageRepository repository;

  @Test
  public void shouldReturnEmptyListWhenIdListIsEmpty() {
    List<DbPackage> packages = repository.findByIds(Collections.emptyList(), null, null).join();
    assertThat(packages, empty());
  }

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.PACKAGE)
      .count(25).offset(0).build();
    List<DbPackage> packages = repository.findByTagFilter(filter, null, STUB_TENANT).join();
    assertThat(packages, empty());
  }

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmptyAndProviderIdIsPresent() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordIdPrefix(STUB_VENDOR_ID).recordType(RecordType.PACKAGE)
      .count(25).offset(0).build();
    List<DbPackage> packages = repository.findByTagFilter(filter, null, null).join();
    assertThat(packages, empty());
  }
}
