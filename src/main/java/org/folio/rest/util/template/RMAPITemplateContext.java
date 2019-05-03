package org.folio.rest.util.template;

import lombok.Value;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.TitlesHoldingsIQService;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.ResourcesServiceImpl;

@Value
public class RMAPITemplateContext {
  private HoldingsIQService holdingsService;
  private PackageServiceImpl packagesService;
  private ProvidersServiceImpl providersService;
  private ResourcesServiceImpl resourcesService;
  private TitlesHoldingsIQService titlesService;
  private OkapiData okapiData;
  private Configuration configuration;
}
