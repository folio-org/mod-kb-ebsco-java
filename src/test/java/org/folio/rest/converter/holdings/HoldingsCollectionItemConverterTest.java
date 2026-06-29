package org.folio.rest.converter.holdings;

import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.PublicationType;
import org.junit.jupiter.api.Test;

class HoldingsCollectionItemConverterTest {

  private final HoldingCollectionItemConverter holdingCollectionItemConverter = new HoldingCollectionItemConverter();

  @Test
  void shouldConvertHoldingToResource() {
    var holding = getStubHolding();
    var resourceCollectionItem = holdingCollectionItemConverter.convert(holding);
    assertNotNull(resourceCollectionItem);
    assertEquals("123356-3157070-19412030", resourceCollectionItem.getId());
    assertEquals("Test Title", resourceCollectionItem.getAttributes().getName());
    assertEquals(19412030, resourceCollectionItem.getAttributes().getTitleId());
    assertEquals("Test one Press", resourceCollectionItem.getAttributes().getPublisherName());
    assertEquals(PublicationType.BOOK, resourceCollectionItem.getAttributes().getPublicationType());
  }

  private static DbHoldingInfo getStubHolding() {
    return readJsonFile("responses/kb-ebsco/holdings/custom-holding.json", DbHoldingInfo.class);
  }
}
