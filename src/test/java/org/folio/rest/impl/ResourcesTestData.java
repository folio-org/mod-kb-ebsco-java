package org.folio.rest.impl;

import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_PACKAGE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_VENDOR_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID_2;

import java.util.Collections;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.ProxyUrl;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.holdingsiq.model.VisibilityInfo;
import org.folio.rest.jaxrs.model.ResourcePutData;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rmapi.result.ResourceResult;

public class ResourcesTestData {
  public static final String OLD_PROXY_ID = "<n>";
  public static final String OLD_COVERAGE_STATEMENT = "statement";
  public static final String OLD_URL = "http://example.com";
  public static final boolean OLD_VISIBILITY_DATA = true;
  public static final String OLD_BEGIN_COVERAGE = "2002-10-10";
  public static final String OLD_END_COVERAGE = "2003-10-10";
  public static final String OLD_EMBARGO_UNIT = "Day";
  public static final int OLD_EMBARGO_VALUE = 5;

  public static final String STUB_MANAGED_RESOURCE_ID =
    STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "-" + STUB_MANAGED_TITLE_ID;
  public static final String STUB_MANAGED_RESOURCE_ID_2 =
    STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "-" + STUB_MANAGED_TITLE_ID_2;
  public static final String STUB_MANAGED_RESOURCE_ID_3 =
    STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_2 + "-" + STUB_MANAGED_TITLE_ID_2;
  public static final String STUB_CUSTOM_RESOURCE_ID =
    STUB_CUSTOM_VENDOR_ID + "-" + STUB_CUSTOM_PACKAGE_ID + "-" + STUB_CUSTOM_TITLE_ID;

  public static ResourcePutRequest getResourcePutRequest(ResourcePutDataAttributes attributes) {
    return new ResourcePutRequest()
      .withData(new ResourcePutData()
        .withType(ResourcePutData.Type.RESOURCES)
        .withAttributes(attributes));
  }

  public static ResourceResult createResourceData() {
    Title title = Title.builder()
      .contributorsList(Collections.emptyList())
      .customerResourcesList(Collections.singletonList(CustomerResources.builder()
        .coverageStatement(OLD_COVERAGE_STATEMENT)
        .isSelected(false)
        .visibilityData(VisibilityInfo.builder()
          .isHidden(OLD_VISIBILITY_DATA).build())
        .customCoverageList(Collections.singletonList(CoverageDates.builder()
          .beginCoverage(OLD_BEGIN_COVERAGE).endCoverage(OLD_END_COVERAGE).build()))
        .customEmbargoPeriod(EmbargoPeriod.builder()
          .embargoUnit(OLD_EMBARGO_UNIT).embargoValue(OLD_EMBARGO_VALUE).build())
        .proxy(ProxyUrl.builder().proxiedUrl(OLD_URL)
          .id(OLD_PROXY_ID).inherited(true).build())
        .url(OLD_URL)
        .userDefinedFields(UserDefinedFields.builder().build())
        .build()
      ))
      .identifiersList(Collections.emptyList())
      .subjectsList(Collections.emptyList())
      .titleId(1)
      .build();
    return new ResourceResult(
      title, null, null, false);
  }
}
