package org.folio.rest.impl;

import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutData;
import org.folio.rest.jaxrs.model.ResourcePutRequest;


public class ResourcesTestData {

  public static ResourcePutRequest getResourcePutRequest(ResourceDataAttributes attributes) {
    return new ResourcePutRequest()
      .withData(new ResourcePutData()
        .withType(ResourcePutData.Type.RESOURCES)
        .withAttributes(attributes));
  }
}
