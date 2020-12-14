package org.folio.rest.converter.costperuse.export;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.service.uc.export.TitleExportModel;

@Component
public class PackageTitlesCollectionConverter {

  @Autowired
  private PackageTitleCostPerUseConverter resourceCostPerUseExportItemConverter;

  public List<TitleExportModel> convert(ResourceCostPerUseCollection resourceCostPerUseCollection, String platform, String year) {
    var data = resourceCostPerUseCollection.getData();
    return mapItems(data, item -> resourceCostPerUseExportItemConverter.convert(item, platform, year));
  }
}
