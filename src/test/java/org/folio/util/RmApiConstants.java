package org.folio.util;

public abstract class RmApiConstants {

  // Credentials
  protected static final String STUB_API_KEY = "TEST_API_KEY";
  protected static final String STUB_CREDENTIALS_ID = "12312312-1231-1231-a111-111111111111";
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";

  // Vendor/Provider IDs and names
  protected static final int STUB_VENDOR_ID = 19;
  protected static final int STUB_VENDOR_ID_2 = 153;
  protected static final int STUB_VENDOR_ID_3 = 167;
  protected static final int CUSTOM_VENDOR_ID = 123356;
  protected static final String STUB_VENDOR_NAME = "Vendor Name1";
  protected static final String STUB_VENDOR_NAME_2 = "Vendor Name2";
  protected static final String STUB_VENDOR_NAME_3 = "Vendor Name3";

  // Package IDs and names
  protected static final int STUB_PACKAGE_ID = 3964;
  protected static final int STUB_PACKAGE_ID_2 = 13964;
  protected static final int STUB_PACKAGE_ID_3 = 23964;
  protected static final int CUSTOM_PACKAGE_ID = 3157070;
  protected static final String STUB_PACKAGE_NAME = "EBSCO Biotechnology Collection: India";
  protected static final String STUB_PACKAGE_NAME_2 = "package 2";
  protected static final String STUB_PACKAGE_NAME_3 = "package 3";
  protected static final String STUB_PACKAGE_CONTENT_TYPE = "AggregatedFullText";

  // Title IDs and names
  protected static final int STUB_TITLE_ID = 985846;
  protected static final int STUB_MANAGED_TITLE_ID = 762169;
  protected static final int STUB_MANAGED_TITLE_ID_2 = 1108525;
  protected static final int CUSTOM_TITLE_ID = 19412030;
  protected static final String STUB_TITLE_NAME = "F. Scott Fitzgerald's The Great Gatsby (Great Gatsby)";
  protected static final String STUB_CUSTOM_TITLE_NAME = "Test Title";

  // Compound resource IDs
  protected static final String STUB_CUSTOM_RESOURCE_ID =
    CUSTOM_VENDOR_ID + "-" + CUSTOM_PACKAGE_ID + "-" + CUSTOM_TITLE_ID;
  protected static final String STUB_MANAGED_RESOURCE_ID =
    STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "-" + STUB_MANAGED_TITLE_ID;
  protected static final String STUB_MANAGED_RESOURCE_ID_2 =
    STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "-" + STUB_MANAGED_TITLE_ID_2;
  protected static final String STUB_MANAGED_RESOURCE_ID_3 =
    STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_2 + "-" + STUB_MANAGED_TITLE_ID_2;

  // Compound package IDs
  protected static final String FULL_PACKAGE_ID = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
  protected static final String FULL_PACKAGE_ID_2 = STUB_VENDOR_ID_2 + "-" + STUB_PACKAGE_ID_2;
  protected static final String FULL_PACKAGE_ID_3 = STUB_VENDOR_ID_3 + "-" + STUB_PACKAGE_ID_3;
  protected static final String FULL_PACKAGE_ID_4 = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_2;
  protected static final String FULL_PACKAGE_ID_5 = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_3;

  // Tags
  protected static final String STUB_TAG_VALUE = "tag one";
  protected static final String STUB_TAG_VALUE_2 = "tag 2";
  protected static final String STUB_TAG_VALUE_3 = "tag 3";
  protected static final String STUB_TAG_VALUE_4 = "tag 4";

  // API URL constants
  protected static final String RM_ACCOUNTS_BASE_URL = "/rm/rmaccounts/";
  protected static final String RM_ACCOUNTS_V2_BASE_URL = "/rm/rmaccounts/v2/";
  protected static final String RM_ACCOUNT_API_PATH = RM_ACCOUNTS_BASE_URL + STUB_CUSTOMER_ID;
  protected static final String RM_ACCOUNT_V2_API_PATH = RM_ACCOUNTS_V2_BASE_URL + STUB_CUSTOMER_ID;
  protected static final String RM_ACCOUNTS_ANY_PATH_REGEX = "/rm/rmaccounts.*";

  protected String providerTagsPath() {
    return "eholdings/providers/" + STUB_VENDOR_ID + "/tags";
  }

  protected String proxiesRmApi() {
    return RM_ACCOUNT_API_PATH + "/proxies.*";
  }

  protected String holdingsStatusRmApi() {
    return RM_ACCOUNT_API_PATH + "/holdings/status";
  }

  protected String postHoldingsRmApi() {
    return RM_ACCOUNT_API_PATH + "/holdings";
  }

  protected String postTransactionsHoldingsRmApi() {
    return RM_ACCOUNT_API_PATH + "/reports/holdings";
  }

  protected String transactionsRmApi() {
    return RM_ACCOUNT_API_PATH + "/reports/holdings/transactions";
  }

  protected String deltasRmApi() {
    return RM_ACCOUNT_API_PATH + "/reports/holdings/deltas";
  }

  protected String rootProxyCustomLabelsRmApi() {
    return RM_ACCOUNT_API_PATH + "/";
  }

  protected String transactionByIdRmApi(String transactionId) {
    return RM_ACCOUNT_API_PATH + "/reports/holdings/transactions/" + transactionId;
  }

  protected String transactionStatusRmApi(String transactionId) {
    return RM_ACCOUNT_API_PATH + "/reports/holdings/transactions/" + transactionId + "/status";
  }

  protected String deltaByIdRmApi(String deltaId) {
    return RM_ACCOUNT_API_PATH + "/reports/holdings/deltas/" + deltaId;
  }

  protected String deltaStatusRmApi(String deltaId) {
    return RM_ACCOUNT_API_PATH + "/reports/holdings/deltas/" + deltaId + "/status";
  }

  protected String resourcesRmApi(int vendorId, int packageId, int titleId) {
    return RM_ACCOUNT_API_PATH + "/vendors/" + vendorId + "/packages/" + packageId + "/titles/" + titleId;
  }

  protected String packageTitlesRmApi(int vendorId, int packageId) {
    return RM_ACCOUNT_API_PATH + "/vendors/" + vendorId + "/packages/" + packageId + "/titles";
  }

  protected String packagesRmApi() {
    return RM_ACCOUNT_V2_API_PATH + "/lists";
  }

  protected String packageRmApi(int packageId) {
    return RM_ACCOUNT_V2_API_PATH + "/lists/" + packageId;
  }

  protected String packageRmApiV1(int vendorId) {
    return RM_ACCOUNT_API_PATH + "/vendors/" + vendorId + "/packages";
  }

  protected String providerPackagesRmApi(int vendorId) {
    return RM_ACCOUNT_V2_API_PATH + "/vendors/" + vendorId + "/lists";
  }

  protected String titlesRmApi() {
    return RM_ACCOUNT_API_PATH + "/titles";
  }

  protected String titlesRmApi(int titleId) {
    return RM_ACCOUNT_API_PATH + "/titles/" + titleId;
  }

  protected String vendorsRmApi() {
    return RM_ACCOUNT_API_PATH + "/vendors";
  }

  protected static String vendorsRmApi(int vendorId) {
    return RM_ACCOUNT_API_PATH + "/vendors/" + vendorId;
  }

  protected String ucCostPerUseTitlesUrl(String year, String month, String currency,
                                         boolean publisherPlatform, boolean previousYear) {
    return "/uc/costperuse/titles?fiscalYear=%s&fiscalMonth=%s&analysisCurrency=%s&publisherPlatform=%s&previousYear=%s"
      .formatted(year, month, currency, publisherPlatform, previousYear);
  }
}
