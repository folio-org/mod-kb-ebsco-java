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
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelsItemCollectionTest {

  @Autowired
  private CustomLabelsItemConverter itemConverter;

  @Test
  public void shouldConvertToCustomLabelOnly() throws URISyntaxException, IOException {

    ObjectMapper mapper = new ObjectMapper();

    RootProxyCustomLabels rootProxy = mapper.readValue(
      getFile("responses/rmapi/custom-labels/get-custom-labels-with-single-element.json"), RootProxyCustomLabels.class);
    final CustomLabelCollectionItem convertedLabel = itemConverter.convert(rootProxy.getLabelList().get(0));
    Integer customLabelId = 1;
    assertEquals(customLabelId, convertedLabel.getAttributes().getId());
    assertEquals("customLabel", convertedLabel.getType());
  }
}
