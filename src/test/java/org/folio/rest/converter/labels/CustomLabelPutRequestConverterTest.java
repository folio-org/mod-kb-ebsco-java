package org.folio.rest.converter.labels;

import static org.junit.Assert.assertEquals;

import static org.folio.test.util.TestUtil.getFile;

import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.util.RestConstants;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelPutRequestConverterTest {

  @Autowired
  private CustomLabelPutRequestConverter putRequestConverter;
  @Test
  public void shouldConvertToCustomLabel() throws URISyntaxException, IOException {

    ObjectMapper mapper = new ObjectMapper();

    CustomLabelPutRequest putRequest = mapper.readValue(
      getFile("requests/kb-ebsco/custom-labels/put-custom-label.json"), CustomLabelPutRequest.class);
    final CustomLabel convertedLabel = putRequestConverter.convert(putRequest);
    Integer customLabelId = 1;
    assertEquals(customLabelId, convertedLabel.getData().getAttributes().getId());
    assertEquals("customLabel", convertedLabel.getData().getType());
    assertEquals(RestConstants.JSONAPI, convertedLabel.getJsonapi());
  }

}
