package org.folio.rmapi.result;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.folio.client.uc.configuration.CommonUcConfiguration;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcPackageCostPerUse;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class ResourceCostPerUseCollectionResult {

  List<DbHoldingInfo> holdingInfos;
  Map<String, UcCostAnalysis> titlePackageCostMap;
  UcPackageCostPerUse packageCostPerUse;
  CommonUcConfiguration configuration;
  PlatformType platformType;
}
