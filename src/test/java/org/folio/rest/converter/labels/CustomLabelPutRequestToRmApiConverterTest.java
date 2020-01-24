package org.folio.rest.converter.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.folio.test.util.TestUtil.getFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class CustomLabelPutRequestToRmApiConverterTest {

  @Autowired
  private CustomLabelPutRequestToRmApiConverter converter;

  @Test
  public void shouldConvertToRootProxyCustomLabelsWithOneLabel() throws URISyntaxException, IOException {
    File resourceFile = getFile("requests/kb-ebsco/custom-labels/put-one-custom-label.json");
    CustomLabelPutRequest putRequest = new ObjectMapper().readValue(resourceFile, CustomLabelPutRequest.class);

    final RootProxyCustomLabels labels = converter.convert(putRequest);

    assertNotNull(labels);
    assertNotNull(labels.getLabelList());
    assertNotNull(labels.getLabelList().get(0));
    assertEquals((Integer) 1, labels.getLabelList().get(0).getId());
    assertEquals("test label 1 updated", labels.getLabelList().get(0).getDisplayLabel());
    assertFalse(labels.getLabelList().get(0).getDisplayOnFullTextFinder());
    assertTrue(labels.getLabelList().get(0).getDisplayOnPublicationFinder());
  }

  @Test
  public void shouldConvertToRootProxyCustomLabelsWithFiveLabels() throws URISyntaxException, IOException {
    File resourceFile = getFile("requests/kb-ebsco/custom-labels/put-five-custom-labels.json");
    CustomLabelPutRequest putRequest = new ObjectMapper().readValue(resourceFile, CustomLabelPutRequest.class);

    final RootProxyCustomLabels labels = converter.convert(putRequest);

    assertNotNull(labels);
    assertNotNull(labels.getLabelList());
    assertEquals(5, labels.getLabelList().size());

    assertNotNull(labels.getLabelList().get(0));
    assertEquals((Integer) 1, labels.getLabelList().get(0).getId());
    assertEquals("test label 1", labels.getLabelList().get(0).getDisplayLabel());
    assertFalse(labels.getLabelList().get(0).getDisplayOnFullTextFinder());
    assertTrue(labels.getLabelList().get(0).getDisplayOnPublicationFinder());

    assertNotNull(labels.getLabelList().get(4));
    assertEquals((Integer) 5, labels.getLabelList().get(4).getId());
    assertEquals("test label 5", labels.getLabelList().get(4).getDisplayLabel());
    assertFalse(labels.getLabelList().get(4).getDisplayOnFullTextFinder());
    assertTrue(labels.getLabelList().get(4).getDisplayOnPublicationFinder());
  }
}
