package org.folio.rest.converter.labels;

import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class CustomLabelsCollectionConverterTest {

  private final Converter<RootProxyCustomLabels, CustomLabelsCollection> fromRmApiConverter =
    new CustomLabelsCollectionConverter.FromRmApi(new CustomLabelsConverter.FromRmApi());

  private final Converter<List<CustomLabel>, CustomLabelsCollection> fromLabelsListConverter =
    new CustomLabelsCollectionConverter.FromLabelsList();

  @Test
  void shouldConvertFromRmApiToCustomLabelsCollection() {
    var rootProxy = readJsonFile("responses/rmapi/proxiescustomlabels/get-success-response.json",
      RootProxyCustomLabels.class);
    var actual = fromRmApiConverter.convert(rootProxy);

    assertNotNull(actual);
    assertEquals((Integer) 5, actual.getMeta().getTotalResults());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, actual.getData().getFirst().getType());
  }

  @Test
  void shouldConvertFromListToCustomLabelsCollection() {
    var customLabels = readJsonFile("responses/kb-ebsco/custom-labels/get-custom-labels-list.json",
      CustomLabelsCollection.class).getData();

    var actual = fromLabelsListConverter.convert(customLabels);

    assertNotNull(actual);
    assertEquals((Integer) 5, actual.getMeta().getTotalResults());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, actual.getData().getFirst().getType());
  }
}
