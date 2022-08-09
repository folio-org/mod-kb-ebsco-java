package org.folio.rest.impl;

import static org.folio.rest.impl.WireMockTestBase.STUB_CUSTOMER_ID;

public class TitlesTestData {
  public static final String STUB_TITLE_ID = "985846";
  public static final String STUB_TITLE_NAME = "F. Scott Fitzgerald's The Great Gatsby (Great Gatsby)";

  public static final String STUB_CUSTOM_VENDOR_ID = "123356";
  public static final String STUB_CUSTOM_PACKAGE_ID = "3157070";
  public static final String STUB_CUSTOM_TITLE_ID = "19412030";
  public static final String STUB_CUSTOM_TITLE_NAME = "Test Title";

  public static final String STUB_MANAGED_TITLE_ID = "762169";
  public static final String STUB_MANAGED_TITLE_ID_2 = "1108525";

  public static final String CUSTOM_TITLE_ENDPOINT =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;
  public static final String CUSTOM_RESOURCE_ENDPOINT =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_CUSTOM_VENDOR_ID + "/packages/" + STUB_CUSTOM_PACKAGE_ID
      + "/titles/" + STUB_CUSTOM_TITLE_ID;
}
