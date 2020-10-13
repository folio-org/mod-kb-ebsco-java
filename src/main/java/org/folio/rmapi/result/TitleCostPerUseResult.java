package org.folio.rmapi.result;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class TitleCostPerUseResult {

  String titleId;
  UCTitleCostPerUse ucTitleCostPerUse;
  Map<String, UCCostAnalysis> titlePackageCostMap;
  List<CustomerResources> customerResources;
  CommonUCConfiguration configuration;
  PlatformType platformType;
}
