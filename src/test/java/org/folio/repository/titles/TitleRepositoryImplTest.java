package org.folio.repository.titles;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.spring.config.TestConfig;
import org.folio.tag.repository.titles.DbTitle;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class TitleRepositoryImplTest {
  @Autowired
  TitlesRepository repository;

  @Test
  public void shouldReturnZeroWhenTagListIsEmpty() {
    int count = repository.countTitlesByResourceTags(Collections.emptyList(), STUB_TENANT).join();
    assertEquals(0, count);
  }

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    List<DbTitle> titles = repository.getTitlesByResourceTags(Collections.emptyList(), 1, 25, STUB_TENANT).join();
    assertThat(titles, empty());
  }
}
