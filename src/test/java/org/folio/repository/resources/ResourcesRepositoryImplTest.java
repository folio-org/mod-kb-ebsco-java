package org.folio.repository.resources;

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
class ResourcesRepositoryImplTest {

  @Autowired
  private ResourceRepository repository;

  @Test
  void shouldReturnEmptyListWhenTagListIsEmpty() {
    var filter = TagFilter.builder().tags(Collections.emptyList())
      .recordIdPrefix("package-id").recordType(RecordType.RESOURCE)
      .count(25).offset(0).build();
    var resources = repository.findByTagFilter(filter, null, STUB_TENANT).join();
    assertTrue(resources.isEmpty());
  }
}
