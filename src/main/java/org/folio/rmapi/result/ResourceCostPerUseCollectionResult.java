package org.folio.rmapi.result;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCPackageCostPerUse;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class ResourceCostPerUseCollectionResult {

  List<DbHoldingInfo> holdingInfos;
  Map<String, UCCostAnalysis> titlePackageCostMap;
  UCPackageCostPerUse packageCostPerUse;
  CommonUCConfiguration configuration;
  PlatformType platformType;
}
