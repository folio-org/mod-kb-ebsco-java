package org.folio.rest.converter.common.attr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.holdingsiq.model.Visibility;
import org.junit.jupiter.api.Test;

class ResourceVisibilityConverterTest {

  private final ResourceVisibilityConverter converter = new ResourceVisibilityConverter();

  @Test
  void shouldBeVisibleWhenAllCategoriesAreVisible() {
    var visibilityData = converter.convert(List.of(
      new Visibility("PF", false, ""),
      new Visibility("FTF", false, ""),
      new Visibility("MARC", false, "")
    ));

    assertFalse(visibilityData.getIsHidden());
    assertEquals("", visibilityData.getReason());
  }

  @Test
  void shouldBeHiddenWhenAtLeastOneCategoryIsHidden() {
    var visibilityData = converter.convert(List.of(
      new Visibility("PF", false, ""),
      new Visibility("FTF", true, "Hidden by customer"),
      new Visibility("MARC", false, "")
    ));

    assertTrue(visibilityData.getIsHidden());
    assertEquals("", visibilityData.getReason());
  }

  @Test
  void shouldMapHiddenByEpReasonOfHiddenCategory() {
    var visibilityData = converter.convert(List.of(
      new Visibility("PF", false, ""),
      new Visibility("MARC", true, "Hidden by EP")
    ));

    assertTrue(visibilityData.getIsHidden());
    assertEquals("Set by system", visibilityData.getReason());
  }
}
