package org.folio.rest.converter.labels;

import static org.folio.test.util.TestUtil.readJsonFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

public class CustomLabelsItemCollectionTest {

  private final Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> itemConverter = new
    CustomLabelsConverter.FromRmApi();

  @Test
  public void shouldConvertToCustomLabelOnly() throws URISyntaxException, IOException {
    org.folio.holdingsiq.model.CustomLabel rmCustomLabel =
      readJsonFile("responses/rmapi/proxiescustomlabels/get-success-response.json",
        RootProxyCustomLabels.class).getLabelList().getFirst();

    CustomLabel actual = itemConverter.convert(rmCustomLabel);

    assertNotNull(actual);
    assertEquals((Integer) 1, actual.getAttributes().getId());
    assertEquals(CustomLabel.Type.CUSTOM_LABELS, actual.getType());
  }
}
