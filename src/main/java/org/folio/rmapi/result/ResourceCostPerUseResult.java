package org.folio.rmapi.result;

import lombok.Builder;
import lombok.Value;
import org.folio.client.uc.configuration.CommonUcConfiguration;
import org.folio.client.uc.model.UcTitleCostPerUse;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class ResourceCostPerUseResult {

  ResourceId resourceId;
  UcTitleCostPerUse ucTitleCostPerUse;
  CommonUcConfiguration configuration;
  PlatformType platformType;
}
