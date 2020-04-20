package org.folio.rest.converter.labels;

import static org.folio.common.ListUtils.mapItems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;

@Component
public class CustomLabelsCollectionConverter implements Converter<RootProxyCustomLabels, CustomLabelsCollection> {

  @Autowired
  private Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> customLabelConverter;

  @Override
  public CustomLabelsCollection convert(@NonNull RootProxyCustomLabels customLabels) {
    return new CustomLabelsCollection()
      .withData(mapItems(customLabels.getLabelList(), customLabelConverter::convert))
      .withMeta(new MetaTotalResults().withTotalResults(customLabels.getLabelList().size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
