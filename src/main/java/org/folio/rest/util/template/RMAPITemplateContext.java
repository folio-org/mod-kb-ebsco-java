package org.folio.rest.util.template;

import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.rmapi.RMAPIService;

@Value
@Builder(toBuilder = true)
public class RMAPITemplateContext {
  private RMAPIService service;
  private OkapiData okapiData;
  private Configuration configuration;
}
