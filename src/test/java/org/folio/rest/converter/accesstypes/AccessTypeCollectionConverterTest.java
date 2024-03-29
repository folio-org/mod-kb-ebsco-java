package org.folio.rest.converter.accesstypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.util.AccessTypesTestUtil;
import org.junit.Test;

public class AccessTypeCollectionConverterTest {

  @Test
  public void testConvertCollection() {
    List<AccessType> accessTypes = AccessTypesTestUtil.testData();

    AccessTypeCollection convertedCollection = new AccessTypeCollectionConverter().convert(accessTypes);

    assertNotNull(convertedCollection);
    assertNotNull(convertedCollection.getData());
    assertNotNull(convertedCollection.getMeta());
    assertNotNull(convertedCollection.getJsonapi());

    assertEquals(3, convertedCollection.getData().size());
    assertEquals(Integer.valueOf(3), convertedCollection.getMeta().getTotalResults());
    assertEquals(accessTypes, convertedCollection.getData());
  }
}
