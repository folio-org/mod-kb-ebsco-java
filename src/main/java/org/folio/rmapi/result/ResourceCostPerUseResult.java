package org.folio.rmapi.result;

import lombok.Builder;
import lombok.Value;

import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.jaxrs.model.PlatformType;

@Value
@Builder
public class ResourceCostPerUseResult {

  ResourceId resourceId;
  UCTitleCostPerUse ucTitleCostPerUse;
  CommonUCConfiguration configuration;
  PlatformType platformType;
}
