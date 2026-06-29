package org.folio.rest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.ValidationException;
import org.folio.holdingsiq.model.ResourceId;
import org.junit.jupiter.api.Test;

class IdParserTest {

  @Test
  void parseResourceIdWhenIdIsValid() {
    ResourceId resourceId = IdParser.parseResourceId("1-2-3");
    assertEquals(1, resourceId.providerIdPart());
    assertEquals(2, resourceId.packageIdPart());
    assertEquals(3, resourceId.titleIdPart());
  }

  @Test
  void parseResourceIdThrowsExceptionWhenIdIsInvalid() {
    assertThrows(ValidationException.class, () ->
      IdParser.parseResourceId("a-b-c"));
  }

  @Test
  void parseResourceIdThrowsExceptionWhenIdIsMissing() {
    assertThrows(ValidationException.class, () ->
      IdParser.parseResourceId(""));
  }

  @Test
  void parseTitleIdWhenIdIsValid() {
    long titleId = IdParser.parseTitleId("123");
    assertEquals(123, titleId);
  }
}
