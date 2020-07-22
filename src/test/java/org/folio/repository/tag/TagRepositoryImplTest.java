package org.folio.repository.tag;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
public class TagRepositoryImplTest {

  @Autowired
  private TagRepository repository;

  @Test
  public void shouldReturnZeroWhenTagListIsEmptyOnCountRecordsByTags() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.RESOURCE)
      .build();
    int count = repository.countRecordsByTagFilter(filter, STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  public void shouldReturnZeroWhenTagListIsEmptyOnCountRecordsByTagsAndPrefix() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.RESOURCE)
      .recordIdPrefix("123")
      .build();
    int count = repository.countRecordsByTagFilter(filter, STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  public void shouldReturnEmptyListWhenIdListIsEmptyOnFindByRecordByIds() {
    List<DbTag> tags = repository.findByRecordByIds(STUB_TENANT, emptyList(), RecordType.RESOURCE).join();
    assertThat(tags, empty());
  }

  @Test
  public void shouldReturnEmptyMapWhenIdListIsEmptyOnFindByRecordByIds() {
    Map<String, List<DbTag>> tags = repository.findPerRecord(STUB_TENANT, emptyList(), RecordType.RESOURCE).join();
    assertThat(tags.keySet(), empty());
  }
}
