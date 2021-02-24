package org.folio.repository.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

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
public class ProviderRepositoryImplTest {

  @Autowired
  ProviderRepository repository;

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.PROVIDER)
      .build();
    List<Long> providerIds = repository.findIdsByTagFilter(filter, null, STUB_TENANT).join();
    assertThat(providerIds, empty());
  }
}
