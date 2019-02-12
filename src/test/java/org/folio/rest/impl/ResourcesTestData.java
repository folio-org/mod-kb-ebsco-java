package org.folio.rest.impl;

import java.util.Collections;

import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutData;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.VisibilityInfo;
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

  public static ResourcePutRequest getResourcePutRequest(ResourceDataAttributes attributes) {
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
        .customEmbargoPeriod(org.folio.rmapi.model.EmbargoPeriod.builder()
          .embargoUnit(OLD_EMBARGO_UNIT).embargoValue(OLD_EMBARGO_VALUE).build())
        .proxy(org.folio.rmapi.model.Proxy.builder()
          .id(OLD_PROXY_ID).inherited(true).build())
        .url(OLD_URL)
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
