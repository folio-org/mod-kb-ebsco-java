package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.jaxrs.model.RootProxyPutData;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.junit.Test;

public class RootProxyPutBodyValidatorTest {
  private final RootProxyPutBodyValidator validator = new RootProxyPutBodyValidator();
  
  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenProxyTypeIdIsNull() {
    
    RootProxyPutRequest request = new RootProxyPutRequest()
        .withData(new RootProxyPutData().withAttributes(new RootProxyDataAttributes().withProxyTypeId(null)));
    validator.validate(request);
  }

  @Test
  public void shouldNotThrowExceptionWhenProxyTypeIdIsSomeNonNullValue() {
    RootProxyPutRequest request = new RootProxyPutRequest()
        .withData(new RootProxyPutData().withAttributes(new RootProxyDataAttributes().withProxyTypeId("hello")));
    validator.validate(request);
  }

  @Test
  public void shouldNotThrowExceptionWhenProxyTypeIdIsEmptyString() {
    RootProxyPutRequest request = new RootProxyPutRequest()
        .withData(new RootProxyPutData().withAttributes(new RootProxyDataAttributes().withProxyTypeId("")));
    validator.validate(request);
  }
}

