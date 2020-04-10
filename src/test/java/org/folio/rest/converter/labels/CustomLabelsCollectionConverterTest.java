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

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelsCollectionConverterTest {

  @Autowired
  private CustomLabelsCollectionConverter itemConverter;

  @Test
  public void shouldConvertToCustomLabelOnly() throws URISyntaxException, IOException {
    RootProxyCustomLabels rootProxy = readJsonFile("responses/rmapi/custom-labels/get-custom-labels.json",
      RootProxyCustomLabels.class);
    final CustomLabelsCollection convertedLabel = itemConverter.convert(rootProxy);

    assertNotNull(convertedLabel);
    assertEquals((Integer) 5, convertedLabel.getMeta().getTotalResults());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, convertedLabel.getData().get(0).getType());
  }
}
