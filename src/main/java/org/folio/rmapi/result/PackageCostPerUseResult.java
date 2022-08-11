package org.folio.rmapi.result;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.folio.client.uc.configuration.CommonUcConfiguration;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcPackageCostPerUse;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class PackageCostPerUseResult {

  String packageId;
  UcPackageCostPerUse ucPackageCostPerUse;
  Map<String, UcCostAnalysis> titlePackageCostMap;
  CommonUcConfiguration configuration;
  PlatformType platformType;
}
