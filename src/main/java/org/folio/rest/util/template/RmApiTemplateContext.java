package org.folio.rest.util.template;

import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.LoadService;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.ResourcesServiceImpl;
import org.folio.rmapi.TitlesServiceImpl;

@Value
@Builder(toBuilder = true)
public class RmApiTemplateContext {

  HoldingsIQService holdingsService;
  PackageServiceImpl packagesService;
  ProvidersServiceImpl providersService;
  ResourcesServiceImpl resourcesService;
  TitlesServiceImpl titlesService;
  LoadService loadingService;
  OkapiData okapiData;
  Configuration configuration;
  String credentialsId;
  String credentialsName;
}
