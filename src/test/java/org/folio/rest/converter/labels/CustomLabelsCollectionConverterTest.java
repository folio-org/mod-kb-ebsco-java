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

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelsCollectionConverterTest {

  @Autowired
  private CustomLabelsCollectionConverter itemConverter;

  @Test
  public void shouldConvertToCustomLabelOnly() throws URISyntaxException, IOException {

    ObjectMapper mapper = new ObjectMapper();

    RootProxyCustomLabels rootProxy = mapper.readValue(
      getFile("responses/rmapi/custom-labels/get-custom-labels.json"), RootProxyCustomLabels.class);
    final CustomLabelsCollection convertedLabel = itemConverter.convert(rootProxy);
    Integer totalSize = 5;
    assertEquals(totalSize, convertedLabel.getMeta().getTotalResults());
    assertEquals("customLabel", convertedLabel.getData().get(0).getType());
  }
}
