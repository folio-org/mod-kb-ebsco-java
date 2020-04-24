package org.folio.rest.impl;

import static org.folio.rest.impl.WireMockTestBase.STUB_CUSTOMER_ID;

public class RmApiConstants {

  public static final String RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/";
  public static final String RMAPI_PROXIES_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/proxies.*";

}
