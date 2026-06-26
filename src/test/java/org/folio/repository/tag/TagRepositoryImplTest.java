package org.folio.repository.tag;

import static java.util.Collections.emptyList;
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
class TagRepositoryImplTest {

  @Autowired
  private TagRepository repository;

  @Test
  void shouldReturnZeroWhenTagListIsEmptyOnCountRecordsByTags() {
    var filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.RESOURCE)
      .build();
    int count = repository.countRecordsByTagFilter(filter, STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  void shouldReturnZeroWhenTagListIsEmptyOnCountRecordsByTagsAndPrefix() {
    var filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.RESOURCE)
      .recordIdPrefix("123")
      .build();
    int count = repository.countRecordsByTagFilter(filter, STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  void shouldReturnEmptyListWhenIdListIsEmptyOnFindByRecordByIds() {
    var tags = repository.findByRecordByIds(STUB_TENANT, emptyList(), RecordType.RESOURCE).join();
    assertTrue(tags.isEmpty());
  }

  @Test
  void shouldReturnEmptyMapWhenIdListIsEmptyOnFindByRecordByIds() {
    var tags = repository.findPerRecord(STUB_TENANT, emptyList(), RecordType.RESOURCE).join();
    assertTrue(tags.isEmpty());
  }
}
