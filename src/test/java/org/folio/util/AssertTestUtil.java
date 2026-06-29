package org.folio.util;

import static org.folio.util.RmApiConstants.CUSTOM_TITLE_ID;
import static org.folio.util.RmApiConstants.FULL_PACKAGE_ID;
import static org.folio.util.RmApiConstants.STUB_CUSTOM_RESOURCE_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.util.IdParser;
import org.skyscreamer.jsonassert.JSONAssert;

@UtilityClass
public final class AssertTestUtil {

  public static void assertEqualsUuid(String string, UUID uuid) {
    assertEquals(string, uuid.toString());
  }

  public static void assertEqualsLong(Long l) {
    assertEquals(CUSTOM_TITLE_ID, l);
  }

  public static void assertEqualsPackageId(PackageId id) {
    assertEquals(FULL_PACKAGE_ID, IdParser.packageIdToString(id));
  }

  public static void assertEqualsResourceId(ResourceId id) {
    assertEquals(STUB_CUSTOM_RESOURCE_ID, IdParser.resourceIdToString(id));
  }

  public static void assertErrorContainsTitle(JsonapiError error, String substring) {
    assertEquals(1, error.getErrors().size());
    assertThat(error.getErrors().getFirst().getTitle(), containsString(substring));
  }

  public static void assertErrorContainsDetail(JsonapiError error, String substring) {
    assertEquals(1, error.getErrors().size());
    assertThat(error.getErrors().getFirst().getDetail(), containsString(substring));
  }

  @SneakyThrows
  public static void assertJsonEqual(String expected, String actual) {
    JSONAssert.assertEquals(expected, actual, false);
  }

  @SneakyThrows
  public static void assertJsonEqual(String expected, String actual, boolean strict) {
    JSONAssert.assertEquals(expected, actual, strict);
  }
}
