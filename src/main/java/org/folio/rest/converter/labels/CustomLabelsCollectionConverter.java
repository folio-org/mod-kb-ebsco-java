package org.folio.rest.converter.labels;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class CustomLabelsCollectionConverter {

  private CustomLabelsCollectionConverter() {

  }

  @Component
  public static class FromRmApi implements Converter<RootProxyCustomLabels, CustomLabelsCollection> {

    private final Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> customLabelConverter;

    public FromRmApi(Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> customLabelConverter) {
      this.customLabelConverter = customLabelConverter;
    }

    @Override
    public CustomLabelsCollection convert(@NonNull RootProxyCustomLabels customLabels) {
      return new CustomLabelsCollection()
        .withData(mapItems(customLabels.getLabelList(), customLabelConverter::convert))
        .withMeta(new MetaTotalResults().withTotalResults(customLabels.getLabelList().size()))
        .withJsonapi(RestConstants.JSONAPI);
    }
  }

  @Component
  public static class FromLabelsList implements Converter<List<CustomLabel>, CustomLabelsCollection> {

    @Override
    public CustomLabelsCollection convert(@NonNull List<CustomLabel> customLabels) {
      return new CustomLabelsCollection()
        .withData(customLabels)
        .withMeta(new MetaTotalResults().withTotalResults(customLabels.size()))
        .withJsonapi(RestConstants.JSONAPI);
    }
  }
}
