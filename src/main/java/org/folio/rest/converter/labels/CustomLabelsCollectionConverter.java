package org.folio.rest.converter.labels;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;

@Component
public class CustomLabelsCollectionConverter implements Converter<RootProxyCustomLabels, CustomLabelsCollection> {

  @Autowired
  private CustomLabelsItemConverter labelsItemConverter;

  @Override
  public CustomLabelsCollection convert(@NonNull RootProxyCustomLabels customLabels) {
    List<CustomLabelCollectionItem> customLabelCollectionItems = mapItems(customLabels.getLabelList(), labelsItemConverter::convert);
    return new org.folio.rest.jaxrs.model.CustomLabelsCollection()
      .withData(customLabelCollectionItems)
      .withMeta(new MetaTotalResults().withTotalResults(customLabels.getLabelList().size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
