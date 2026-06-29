package org.folio.rest.converter.accesstypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.util.AccessTypesTestUtil;
import org.junit.jupiter.api.Test;

class AccessTypeCollectionConverterTest {

  @Test
  void convertCollection() {
    var accessTypes = AccessTypesTestUtil.testData();

    var convertedCollection = new AccessTypeCollectionConverter().convert(accessTypes);

    assertNotNull(convertedCollection);
    assertNotNull(convertedCollection.getData());
    assertNotNull(convertedCollection.getMeta());
    assertNotNull(convertedCollection.getJsonapi());

    assertEquals(3, convertedCollection.getData().size());
    assertEquals(Integer.valueOf(3), convertedCollection.getMeta().getTotalResults());
    assertEquals(accessTypes, convertedCollection.getData());
  }
}
