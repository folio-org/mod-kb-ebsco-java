package org.folio.rest.converter.labels;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;

@Component
public class CustomLabelPutRequestConverter implements Converter<CustomLabelPutRequest, CustomLabelsCollection> {

  @Override
  public CustomLabelsCollection convert(CustomLabelPutRequest customLabelPutRequest) {
    return new CustomLabelsCollection()
        .withData(customLabelPutRequest.getData())
        .withMeta(new MetaTotalResults().withTotalResults(customLabelPutRequest.getData().size()))
        .withJsonapi(RestConstants.JSONAPI);
  }
}
