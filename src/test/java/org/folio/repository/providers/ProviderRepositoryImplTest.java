package org.folio.repository.providers;

import static org.folio.util.TestUtil.STUB_TENANT;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.folio.repository.RecordType;
import org.folio.rest.model.filter.TagFilter;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class ProviderRepositoryImplTest {

  @Autowired
  private ProviderRepository repository;

  @Test
  void shouldReturnEmptyListWhenTagListIsEmpty() {
    var filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.PROVIDER)
      .build();
    var providerIds = repository.findIdsByTagFilter(filter, null, STUB_TENANT).join();
    assertTrue(providerIds.isEmpty());
  }
}
