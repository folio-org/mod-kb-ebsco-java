package org.folio.rest.converter.accesstypes;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.util.AccessTypesTestUtil;

public class AccessTypeCollectionConverterTest {

  @Test
  public void testConvertCollection() {
    List<AccessTypeCollectionItem> accessTypeCollectionItems = AccessTypesTestUtil.testData();

    AccessTypeCollection convertedCollection = new AccessTypeCollectionConverter().convert(accessTypeCollectionItems);

    assertNotNull(convertedCollection);
    assertNotNull(convertedCollection.getData());
    assertNotNull(convertedCollection.getMeta());
    assertNotNull(convertedCollection.getJsonapi());

    assertEquals(3, convertedCollection.getData().size());
    assertEquals(Integer.valueOf(3), convertedCollection.getMeta().getTotalResults());
    assertEquals(accessTypeCollectionItems, convertedCollection.getData());
  }
}
