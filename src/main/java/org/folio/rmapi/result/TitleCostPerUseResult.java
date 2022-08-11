package org.folio.rmapi.result;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.folio.client.uc.configuration.CommonUcConfiguration;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcTitleCostPerUse;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class TitleCostPerUseResult {

  String titleId;
  UcTitleCostPerUse ucTitleCostPerUse;
  Map<String, UcCostAnalysis> titlePackageCostMap;
  List<CustomerResources> customerResources;
  CommonUcConfiguration configuration;
  PlatformType platformType;
}
