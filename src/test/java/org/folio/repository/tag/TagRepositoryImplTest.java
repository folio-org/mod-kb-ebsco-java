package org.folio.repository.tag;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.repository.RecordType;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class TagRepositoryImplTest {

  @Autowired
  private TagRepository repository;

  @Test
  public void shouldReturnZeroWhenTagListIsEmptyOnCountRecordsByTags() {
    int count = repository.countRecordsByTags(emptyList(), RecordType.RESOURCE, UUID.randomUUID(), STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  public void shouldReturnZeroWhenTagListIsEmptyOnCountRecordsByTagsAndPrefix() {
    int count = repository.countRecordsByTagsAndPrefix(emptyList(), "123-", STUB_TENANT, RecordType.RESOURCE).join();
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
