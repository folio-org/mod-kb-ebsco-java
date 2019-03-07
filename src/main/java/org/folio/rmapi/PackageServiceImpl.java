package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.TitlesHoldingsIQService;
import org.folio.holdingsiq.service.impl.PackagesHoldingsIQServiceImpl;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.VendorResult;

public class PackageServiceImpl extends PackagesHoldingsIQServiceImpl {

  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_RESOURCES_VALUE = "resources";

  private ProvidersServiceImpl providerService;
  private TitlesHoldingsIQService titlesService;

  public PackageServiceImpl(Configuration config, Vertx vertx, ProvidersServiceImpl providerService, TitlesHoldingsIQService titlesService) {
    super(config, vertx);
    this.providerService = providerService;
    this.titlesService = titlesService;
  }

  public CompletableFuture<PackageResult> retrievePackage(PackageId packageId, List<String> includedObjects) {
    CompletableFuture<PackageByIdData> packageFuture = retrievePackage(packageId);

    CompletableFuture<Titles> titlesFuture;
    if (includedObjects.contains(INCLUDE_RESOURCES_VALUE)) {
      titlesFuture = titlesService.retrieveTitles(packageId.getProviderIdPart(), packageId.getPackageIdPart(), FilterQuery.builder().build(),
        Sort.NAME, 1, 25);
    } else {
      titlesFuture = completedFuture(null);
    }

    CompletableFuture<VendorResult> vendorFuture;
    if (includedObjects.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = providerService.retrieveProvider(packageId.getProviderIdPart(), null);
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }

    return CompletableFuture.allOf(packageFuture, titlesFuture, vendorFuture)
      .thenCompose(o ->
        completedFuture(new PackageResult(packageFuture.join(), vendorFuture.join().getVendor(), titlesFuture.join())));
  }

}
