package org.folio.repository.titles;

import static org.folio.util.TestUtil.STUB_TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class TitleRepositoryImplTest {

  @Autowired
  private TitlesRepository repository;

  @Test
  void shouldReturnZeroWhenTagListIsEmpty() {
    int count = repository.countTitlesByResourceTags(Collections.emptyList(), null, STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  void shouldReturnEmptyListWhenTagListIsEmpty() {
    var filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.RESOURCE)
      .build();
    var titles = repository.findByTagFilter(filter, null, STUB_TENANT).join();
    assertTrue(titles.isEmpty());
  }
}
