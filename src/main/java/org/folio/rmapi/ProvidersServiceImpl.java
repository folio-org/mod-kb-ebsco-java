package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ProviderHoldingsIQServiceImpl;
import org.folio.rmapi.result.VendorResult;

public class ProvidersServiceImpl extends ProviderHoldingsIQServiceImpl {

  private static final String INCLUDE_PACKAGES_VALUE = "packages";

  private PackagesHoldingsIQService packagesService;

  public ProvidersServiceImpl(String customerId, String apiKey, String baseURI, Vertx vertx, HoldingsIQService holdingsService) {
    super(customerId, apiKey, baseURI, vertx, holdingsService);
  }

  public void setPackagesService(PackagesHoldingsIQService packagesService) {
    this.packagesService = packagesService;
  }

  public CompletableFuture<VendorResult> retrieveProvider(long id, String include) {

    CompletableFuture<VendorById> vendorFuture;
    CompletableFuture<Packages> packagesFuture;

    vendorFuture = super.retrieveProvider(id);
    if (INCLUDE_PACKAGES_VALUE.equalsIgnoreCase(include)) {
      packagesFuture = packagesService.retrievePackages(id);
    } else {
      packagesFuture = completedFuture(null);
    }
    return CompletableFuture.allOf(vendorFuture, packagesFuture)
      .thenCompose(o ->
        completedFuture(new VendorResult(vendorFuture.join(), packagesFuture.join())));
  }

}
