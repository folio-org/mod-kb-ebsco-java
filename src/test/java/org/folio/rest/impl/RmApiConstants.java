package org.folio.rest.impl;

import static org.folio.rest.impl.WireMockTestBase.STUB_CUSTOMER_ID;

public class RmApiConstants {

  public static final String RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/";
  public static final String RMAPI_PROXIES_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/proxies.*";

  public static final String RMAPI_HOLDINGS_STATUS_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings/status";
  public static final String RMAPI_POST_HOLDINGS_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";

  public static final String RMAPI_TRANSACTION_STATUS_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/transactions/%s/status";
  public static final String RMAPI_POST_TRANSACTIONS_HOLDINGS_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings";
  public static final String RMAPI_TRANSACTIONS_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/transactions";
  public static final String RMAPI_TRANSACTION_BY_ID_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/transactions/%s";
  public static final String RMAPI_DELTAS_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/deltas";
  public static final String RMAPI_DELTA_BY_ID_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/deltas/%s";
  public static final String RMAPI_DELTA_STATUS_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/deltas/%s/status";

}
