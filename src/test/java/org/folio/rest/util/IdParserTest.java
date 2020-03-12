package org.folio.rest.util;

import static org.junit.Assert.assertEquals;

import javax.validation.ValidationException;

import org.junit.Test;

import org.folio.holdingsiq.model.ResourceId;

public class IdParserTest {

  @Test
  public void parseResourceIdWhenIdIsValid() {
    ResourceId resourceId = IdParser.parseResourceId("1-2-3");
    assertEquals(1, resourceId.getProviderIdPart());
    assertEquals(2, resourceId.getPackageIdPart());
    assertEquals(3, resourceId.getTitleIdPart());
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
