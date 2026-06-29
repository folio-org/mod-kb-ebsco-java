package org.folio.rest.converter.labels;

import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class CustomLabelsItemCollectionTest {

  private final Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> itemConverter = new
    CustomLabelsConverter.FromRmApi();

  @Test
  void shouldConvertToCustomLabelOnly() {
    var rmCustomLabel = readJsonFile("responses/rmapi/proxiescustomlabels/get-success-response.json",
      RootProxyCustomLabels.class).getLabelList().getFirst();

    var actual = itemConverter.convert(rmCustomLabel);

    assertNotNull(actual);
    assertEquals((Integer) 1, actual.getAttributes().getId());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, actual.getType());
  }
}
