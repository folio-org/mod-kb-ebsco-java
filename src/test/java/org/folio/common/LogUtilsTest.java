package org.folio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.LogUtils.collectionToLogMsg;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LogUtilsTest {

  private static final List<String> BIG_LIST = Arrays.asList("One", "Two", "Three");
  private static final List<String> SMALL_LIST = Arrays.asList("One", "Two");
  private static final String MSG = "size of list ";

  @Test
  public void collectionToLogMsg_moreThanThreeItems() {
    var actual = collectionToLogMsg(BIG_LIST);
    assertThat(actual).isEqualTo(MSG + BIG_LIST.size());
  }

  @Test
  public void testCollectionToLogMsg_lessThenThreeItems() {
    var actual = collectionToLogMsg(SMALL_LIST);
    assertThat(actual).isEqualTo(SMALL_LIST.toString());
  }

  @Test
  public void testCollectionToLogMsg_nullOrEmptyList() {
    var actual = collectionToLogMsg(null);
    assertThat(actual).isEqualTo(MSG + 0);
  }
}
