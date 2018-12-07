package org.folio.rest.parser;

import static org.junit.Assert.*;

import javax.validation.ValidationException;

import org.folio.rest.model.ResourceId;
import org.junit.Test;

public class IdParserTest {

  private IdParser idParser = new IdParser();

  @Test
  public void parseResourceIdWhenIdIsValid() {
    ResourceId resourceId = idParser.parseResourceId("1-2-3");
    assertEquals(1, resourceId.getProviderIdPart());
    assertEquals(2, resourceId.getPackageIdPart());
    assertEquals(3, resourceId.getTitleIdPart());
  }

  @Test(expected = ValidationException.class)
  public void parseResourceIdThrowsExceptionWhenIdIsInvalid() {
    idParser.parseResourceId("a-b-c");
  }

  @Test(expected = ValidationException.class)
  public void parseResourceIdThrowsExceptionWhenIdIsMissing() {
    idParser.parseResourceId("");
  }

  @Test
  public void parseTitleIdWhenIdIsValid() {
    long titleId = idParser.parseTitleId("123");
    assertEquals(123, titleId);
  }
}
