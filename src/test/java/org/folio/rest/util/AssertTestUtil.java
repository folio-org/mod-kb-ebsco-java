package org.folio.rest.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_CUSTOM_RESOURCE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_TITLE_ID;

import java.util.UUID;

import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.jaxrs.model.JsonapiError;

public final class AssertTestUtil {

  private AssertTestUtil() {
  }

  public static void assertEqualsUUID(String string, UUID uuid) {
    assertEquals(string, uuid.toString());
  }

  public static void assertEqualsLong(Long l) {
    assertEquals(STUB_CUSTOM_TITLE_ID, String.valueOf(l));
  }

  public static void assertEqualsPackageId(PackageId id) {
    assertEquals(FULL_PACKAGE_ID, IdParser.packageIdToString(id));
  }

  public static void assertEqualsResourceId(ResourceId id) {
    assertEquals(STUB_CUSTOM_RESOURCE_ID, IdParser.resourceIdToString(id));
  }

  public static void assertErrorContainsTitle(JsonapiError error, String substring) {
    assertThat(error.getErrors().get(0).getTitle(), containsString(substring));
  }

  public static void assertErrorContainsDetail(JsonapiError error, String substring) {
    assertThat(error.getErrors().get(0).getDetail(), containsString(substring));
  }
}
