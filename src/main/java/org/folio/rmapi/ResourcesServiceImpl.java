package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ResourcesHoldingsIQServiceImpl;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;

public class ResourcesServiceImpl extends ResourcesHoldingsIQServiceImpl {

  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";

  private ProvidersServiceImpl providerService;
  private PackagesHoldingsIQService packagesService;

  public ResourcesServiceImpl(String customerId, String apiKey, String baseURI, Vertx vertx,
                              ProvidersServiceImpl providerService, PackagesHoldingsIQService packagesService) {
    super(customerId, apiKey, baseURI, vertx);
    this.providerService = providerService;
    this.packagesService = packagesService;
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes) {
    CompletableFuture<Title> titleFuture;
    CompletableFuture<PackageByIdData> packageFuture;
    CompletableFuture<VendorResult> vendorFuture;

    titleFuture = super.retrieveResource(resourceId);
    if (includes.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = providerService.retrieveProvider(resourceId.getProviderIdPart(), "");
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }
    if (includes.contains(INCLUDE_PACKAGE_VALUE)) {
      PackageId id = PackageId.builder()
        .providerIdPart(resourceId.getProviderIdPart())
        .packageIdPart(resourceId.getPackageIdPart()).build();
      packageFuture = packagesService.retrievePackage(id);
    } else {
      packageFuture = completedFuture(null);
    }
    boolean includeTitle = includes.contains("title");

    return CompletableFuture.allOf(titleFuture, vendorFuture, packageFuture)
      .thenCompose(o ->
        completedFuture(new ResourceResult(titleFuture.join(), vendorFuture.join().getVendor(), packageFuture.join(), includeTitle)));
  }
}
