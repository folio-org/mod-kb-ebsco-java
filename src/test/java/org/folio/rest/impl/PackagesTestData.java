package org.folio.rest.impl;

import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID_3;

import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutData;
import org.folio.rest.jaxrs.model.PackagePutRequest;

public class PackagesTestData {

  public static final String STUB_PACKAGE_ID = "3964";
  public static final String STUB_PACKAGE_ID_2 = "13964";
  public static final String STUB_PACKAGE_ID_3 = "23964";

  public static final String STUB_PACKAGE_NAME = "EBSCO Biotechnology Collection: India";
  public static final String STUB_PACKAGE_NAME_2 = "package 2";
  public static final String STUB_PACKAGE_NAME_3 = "package 3";

  public static final String STUB_PACKAGE_CONTENT_TYPE = "AggregatedFullText";

  public static final String FULL_PACKAGE_ID = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
  public static final String FULL_PACKAGE_ID_2 = STUB_VENDOR_ID_2 + "-" + STUB_PACKAGE_ID_2;
  public static final String FULL_PACKAGE_ID_3 = STUB_VENDOR_ID_3 + "-" + STUB_PACKAGE_ID_3;
  public static final String FULL_PACKAGE_ID_4 = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_2;
  public static final String FULL_PACKAGE_ID_5 = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_3;

  public static PackagePutRequest getPackagePutRequest(PackageDataAttributes attributes) {
    return new PackagePutRequest()
      .withData(new PackagePutData()
        .withType(PackagePutData.Type.PACKAGES)
        .withAttributes(attributes));
  }
}
