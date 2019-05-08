package org.folio.rest.impl;

import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutData;
import org.folio.rest.jaxrs.model.PackagePutRequest;

public class PackagesTestData {

  public static PackagePutRequest getPackagePutRequest(PackageDataAttributes attributes) {
    return new PackagePutRequest()
      .withData(new PackagePutData()
        .withType(PackagePutData.Type.PACKAGES)
        .withAttributes(attributes));
  }
}
