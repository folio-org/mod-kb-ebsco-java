package org.folio.rest.util.template;

import org.folio.config.RMAPIConfiguration;
import org.folio.rest.model.OkapiData;
import org.folio.rmapi.RMAPIService;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RMAPITemplateContext {
  private RMAPIService service;
  private OkapiData okapiData;
  private RMAPIConfiguration configuration;
}
