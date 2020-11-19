package org.folio.rest.converter.costperuse.export;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.service.uc.export.TitleExportModel;

@Component
public class PackageTitlesCollectionConverter implements Converter<ResourceCostPerUseCollection, List<TitleExportModel>> {

  @Autowired
  private Converter<ResourceCostPerUseCollectionItem, TitleExportModel> resourceCostPerUseExportItemConverter;
  private static final Logger LOG = LoggerFactory.getLogger(PackageTitlesCollectionConverter.class);

  @Override
  public List<TitleExportModel> convert(ResourceCostPerUseCollection resourceCostPerUseCollection) {
    var data = resourceCostPerUseCollection.getData();
    LOG.info("Start CONVERTING " + data.size() + " elements");
    List<TitleExportModel> titleExportModels = mapItems(data, resourceCostPerUseExportItemConverter::convert);
    LOG.info("Finished CONVERTING " + data.size() + " elements");
    return titleExportModels;
  }
}
