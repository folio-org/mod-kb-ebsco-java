package org.folio.rmapi.result;

import java.util.Map;

import lombok.Builder;
import lombok.Value;

import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCPackageCostPerUse;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class PackageCostPerUseResult {

  String packageId;
  UCPackageCostPerUse ucPackageCostPerUse;
  Map<String, UCCostAnalysis> titlePackageCostMap;
  CommonUCConfiguration configuration;
  PlatformType platformType;
}
