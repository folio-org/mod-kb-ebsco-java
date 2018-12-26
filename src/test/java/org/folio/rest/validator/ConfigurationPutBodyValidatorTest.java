package org.folio.rest.validator;

import static org.hamcrest.Matchers.containsString;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ConfigurationAttributes;
import org.folio.rest.jaxrs.model.ConfigurationData;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigurationPutBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private ConfigurationPutBodyValidator validator = new ConfigurationPutBodyValidator();

  @Test
  public void shouldNotThrowExceptionIfConfigurationIsValid() {
    ConfigurationPutRequest configurationRequest = createConfigurationRequest();
    validator.validate(configurationRequest);
  }

  @Test
  public void shouldThrowExceptionWhenApiKeyIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("API key"));
    ConfigurationPutRequest configurationRequest = createConfigurationRequest();
    configurationRequest.getData().getAttributes().setApiKey(null);
    validator.validate(configurationRequest);
  }

  @Test
  public void shouldThrowExceptionWhenCustomerIdIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("Customer ID"));
    ConfigurationPutRequest configurationRequest = createConfigurationRequest();
    configurationRequest.getData().getAttributes().setCustomerId(null);
    validator.validate(configurationRequest);
  }

  @Test
  public void shouldThrowExceptionWhenUrlIsNull() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("API endpoint"));
    ConfigurationPutRequest configurationRequest = createConfigurationRequest();
    configurationRequest.getData().getAttributes().setRmapiBaseUrl(null);
    validator.validate(configurationRequest);
  }

  private ConfigurationPutRequest createConfigurationRequest() {
    return new ConfigurationPutRequest()
      .withData(new ConfigurationData()
        .withAttributes(new ConfigurationAttributes()
          .withApiKey("123")
          .withCustomerId("customerId")
          .withRmapiBaseUrl("https://url")));
  }
}
