package org.folio.rest.util.template;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.ResourcesServiceImpl;
import org.folio.rmapi.TitlesServiceImpl;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.rmapi.cache.VendorCacheKey;

@Component
@Scope("prototype")
public class RMAPITemplateContextBuilder {
  private OkapiData okapiData;
  private Configuration configuration;
  @Autowired
  private VertxCache<VendorCacheKey, VendorById> vendorCache;
  @Autowired
  private VertxCache<PackageCacheKey, PackageByIdData> packageCache;
  @Autowired
  private VertxCache<ResourceCacheKey, Title> resourceCache;
  private VertxCache<TitleCacheKey, Title> titleCache;

  @Autowired
  private Vertx vertx;

  public RMAPITemplateContextBuilder okapiData(OkapiData okapiData){
    this.okapiData = okapiData;
    return this;
  }
  public RMAPITemplateContextBuilder configuration(Configuration configuration){
    this.configuration = configuration;
    return this;
  }

  public RMAPITemplateContext build(){
    String tenant = okapiData.getTenant();
    final HoldingsIQService holdingsService = new HoldingsIQServiceImpl(configuration, vertx);
    final TitlesServiceImpl titlesService = new TitlesServiceImpl(configuration, vertx, okapiData.getTenant(), titleCache);
    final ProvidersServiceImpl providersService =
      new ProvidersServiceImpl(configuration, vertx, tenant, holdingsService, vendorCache);
    final PackageServiceImpl packagesService =
      new PackageServiceImpl(configuration, vertx, tenant, providersService, titlesService, packageCache);
    final ResourcesServiceImpl resourcesService =
      new ResourcesServiceImpl(configuration, vertx, tenant, providersService, packagesService, resourceCache);
    providersService.setPackagesService(packagesService);
    return new RMAPITemplateContext(holdingsService, packagesService, providersService,
      resourcesService, titlesService, okapiData, configuration);
  }
}