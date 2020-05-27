package org.folio.rest.converter.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.folio.test.util.TestUtil.readJsonFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;

public class CustomLabelsCollectionConverterTest {

  private final Converter<RootProxyCustomLabels, CustomLabelsCollection> fromRmApiConverter =
    new CustomLabelsCollectionConverter.FromRmApi(new CustomLabelsConverter.FromRmApi());

  private final Converter<List<CustomLabel>, CustomLabelsCollection> fromLabelsListConverter =
    new CustomLabelsCollectionConverter.FromLabelsList();

  @Test
  public void shouldConvertFromRmApiToCustomLabelsCollection() throws URISyntaxException, IOException {
    RootProxyCustomLabels rootProxy = readJsonFile("responses/rmapi/proxiescustomlabels/get-success-response.json",
      RootProxyCustomLabels.class);
    CustomLabelsCollection actual = fromRmApiConverter.convert(rootProxy);

    assertNotNull(actual);
    assertEquals((Integer) 5, actual.getMeta().getTotalResults());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, actual.getData().get(0).getType());
  }

  @Test
  public void shouldConvertFromListToCustomLabelsCollection() throws URISyntaxException, IOException {
    List<CustomLabel> rootProxy = readJsonFile("responses/kb-ebsco/custom-labels/get-custom-labels-list.json",
      CustomLabelsCollection.class).getData();

    CustomLabelsCollection actual = fromLabelsListConverter.convert(rootProxy);

    assertNotNull(actual);
    assertEquals((Integer) 5, actual.getMeta().getTotalResults());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, actual.getData().get(0).getType());
  }
}
