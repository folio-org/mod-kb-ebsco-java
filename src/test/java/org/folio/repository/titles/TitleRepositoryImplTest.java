package org.folio.repository.titles;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
public class TitleRepositoryImplTest {
  @Autowired
  TitlesRepository repository;

  @Test
  public void shouldReturnZeroWhenTagListIsEmpty() {
    int count = repository.countTitlesByResourceTags(Collections.emptyList(), null, STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    TagFilter filter = TagFilter.builder().tags(Collections.emptyList())
      .recordType(RecordType.RESOURCE)
      .build();
    List<DbTitle> titles = repository.getTitlesByTagFilter(filter, null, STUB_TENANT).join();
    assertThat(titles, empty());
  }
}
