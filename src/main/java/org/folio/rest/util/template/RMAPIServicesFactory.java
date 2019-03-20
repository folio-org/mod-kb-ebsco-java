package org.folio.rest.util.template;

import io.vertx.core.Vertx;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.TitlesHoldingsIQService;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.holdingsiq.service.impl.TitlesHoldingsIQServiceImpl;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.ResourcesServiceImpl;

@Component
@Qualifier("rmapiServicesFactory")
public class RMAPIServicesFactory {

  public HoldingsIQService createHoldingsService(Configuration config, Vertx vertx){
    return new HoldingsIQServiceImpl(config, vertx);
  }

  public TitlesHoldingsIQService createTitlesHoldingsIQService(Configuration config, Vertx vertx){
    return new TitlesHoldingsIQServiceImpl(config, vertx);
  }

  public ProvidersServiceImpl createProvidersServiceImpl(Configuration config, Vertx vertx){
    ProvidersServiceImpl providersService = new ProvidersServiceImpl(config, vertx, createHoldingsService(config, vertx));
    PackageServiceImpl packageService = new PackageServiceImpl(config, vertx, providersService,
      createTitlesHoldingsIQService(config, vertx));
    providersService.setPackagesService(packageService);
    return providersService;
  }

  public PackageServiceImpl createPackageServiceImpl(Configuration config, Vertx vertx){
    return new PackageServiceImpl(config, vertx, createProvidersServiceImpl(config, vertx), createTitlesHoldingsIQService(config,vertx));
  }

  public ResourcesServiceImpl createResourcesServiceImpl(Configuration config, Vertx vertx){
    return new ResourcesServiceImpl(config, vertx, createProvidersServiceImpl(config, vertx), createPackageServiceImpl(config, vertx));
  }
}
