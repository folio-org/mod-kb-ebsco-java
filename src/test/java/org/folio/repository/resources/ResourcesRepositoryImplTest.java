package org.folio.repository.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
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
public class ResourcesRepositoryImplTest {

  @Autowired
  ResourceRepository repository;

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordIdPrefix(STUB_PACKAGE_ID).recordType(RecordType.RESOURCE)
      .count(25).offset(0).build();
    List<DbResource> resources = repository.findByTagFilter(
      filter, null, STUB_TENANT).join();
    assertThat(resources, empty());
  }
}
