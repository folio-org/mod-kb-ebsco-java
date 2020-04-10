package org.folio.rest.converter.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.folio.test.util.TestUtil.readJsonFile;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.util.RestConstants;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelPutRequestConverterTest {

  @Autowired
  private CustomLabelPutRequestConverter putRequestConverter;

  @Test
  public void shouldConvertToCustomLabelsCollection() throws URISyntaxException, IOException {
    String resourceFile = "requests/kb-ebsco/custom-labels/put-five-custom-labels.json";
    CustomLabelPutRequest putRequest = readJsonFile(resourceFile, CustomLabelPutRequest.class);

    final CustomLabelsCollection convertedCollection = putRequestConverter.convert(putRequest);

    assertNotNull(convertedCollection);
    assertNotNull(convertedCollection.getData());
    assertEquals(5, convertedCollection.getData().size());
    assertEquals(Integer.valueOf(5), convertedCollection.getMeta().getTotalResults());
    assertEquals(RestConstants.JSONAPI, convertedCollection.getJsonapi());

    CustomLabel firstItem = convertedCollection.getData().get(0);
    assertNotNull(firstItem);
    assertEquals(Integer.valueOf(1), firstItem.getAttributes().getId());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, firstItem.getType());

    CustomLabel lastItem = convertedCollection.getData().get(4);
    assertNotNull(lastItem);
    assertEquals(Integer.valueOf(5), lastItem.getAttributes().getId());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, lastItem.getType());
  }

}
