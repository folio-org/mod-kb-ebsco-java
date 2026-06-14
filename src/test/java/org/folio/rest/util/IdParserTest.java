package org.folio.rest.util;

import static org.junit.Assert.assertEquals;

import jakarta.validation.ValidationException;
import org.folio.holdingsiq.model.ResourceId;
import org.junit.Test;

public class IdParserTest {

  @Test
  public void parseResourceIdWhenIdIsValid() {
    ResourceId resourceId = IdParser.parseResourceId("1-2-3");
    assertEquals(1, resourceId.providerIdPart());
    assertEquals(2, resourceId.packageIdPart());
    assertEquals(3, resourceId.titleIdPart());
  }

  @Test(expected = ValidationException.class)
  public void parseResourceIdThrowsExceptionWhenIdIsInvalid() {
    IdParser.parseResourceId("a-b-c");
  }

  @Test(expected = ValidationException.class)
  public void parseResourceIdThrowsExceptionWhenIdIsMissing() {
    IdParser.parseResourceId("");
  }

  @Test
  public void parseTitleIdWhenIdIsValid() {
    long titleId = IdParser.parseTitleId("123");
    assertEquals(123, titleId);
  }
}
