package org.folio.rest.converter.common.attr;

import static org.folio.rest.converter.common.attr.VisibilityDetailsMapper.HIDDEN_BY_CUSTOMER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.rest.jaxrs.model.Visibility.Category;
import org.junit.jupiter.api.Test;

class VisibilityDetailsMapperTest {

  @Test
  void shouldMapVisibleCategory() {
    var result = VisibilityDetailsMapper.toApiVisibility(new org.folio.holdingsiq.model.Visibility("PF", false, ""));

    assertEquals(Category.PF, result.getCategory());
    assertFalse(result.getHidden());
    assertEquals("", result.getReason());
  }

  @Test
  void shouldMapHiddenRequestVisibilityWithHiddenByCustomerReason() {
    var result = VisibilityDetailsMapper.toRmApiVisibility(
      new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.FTF).withHidden(true).withReason("ignored"));

    assertEquals(new org.folio.holdingsiq.model.Visibility("FTF", true, HIDDEN_BY_CUSTOMER), result);
  }

  @Test
  void shouldMapVisibleRequestVisibilityWithoutReason() {
    var result = VisibilityDetailsMapper.toRmApiVisibility(
      new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.PF).withHidden(false).withReason("ignored"));

    assertEquals(new org.folio.holdingsiq.model.Visibility("PF", false, null), result);
  }

  @Test
  void shouldReturnNullRmApiVisibilityDetailsWhenRequestVisibilityIsNullOrEmpty() {
    assertNull(VisibilityDetailsMapper.toRmApiVisibilityDetails(null));
    assertNull(VisibilityDetailsMapper.toRmApiVisibilityDetails(List.of()));
  }

  @Test
  void shouldMapAllRequestVisibilityCategories() {
    var result = VisibilityDetailsMapper.toRmApiVisibilityDetails(List.of(
      new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.PF).withHidden(true),
      new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.MARC).withHidden(false)));

    assertEquals(List.of(new org.folio.holdingsiq.model.Visibility("PF", true, HIDDEN_BY_CUSTOMER),
      new org.folio.holdingsiq.model.Visibility("MARC", false, null)), result);
  }

  @Test
  void shouldMapHiddenCategoryWithReason() {
    var result = VisibilityDetailsMapper.toApiVisibility(
      new org.folio.holdingsiq.model.Visibility("marc", true, "Set by system"));

    assertEquals(Category.MARC, result.getCategory());
    assertTrue(result.getHidden());
    assertEquals("Set by system", result.getReason());
  }
}
