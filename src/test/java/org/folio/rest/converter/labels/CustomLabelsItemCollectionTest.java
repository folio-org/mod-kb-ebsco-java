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
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelsItemCollectionTest {

  @Autowired
  private CustomLabelsConverter itemConverter;

  @Test
  public void shouldConvertToCustomLabelOnly() throws URISyntaxException, IOException {
    RootProxyCustomLabels rootProxy =
      readJsonFile("responses/rmapi/custom-labels/get-custom-labels-with-single-element.json",
        RootProxyCustomLabels.class);
    final CustomLabel convertedLabel = itemConverter.convert(rootProxy.getLabelList().get(0));

    assertNotNull(convertedLabel);
    assertEquals((Integer) 1, convertedLabel.getAttributes().getId());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, convertedLabel.getType());
  }
}
