package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;

public class RMAPIService extends HoldingsIQServiceImpl {

  private static final String INCLUDE_PACKAGES_VALUE = "packages";
  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";
  private static final String INCLUDE_RESOURCES_VALUE = "resources";


  public RMAPIService(String customerId, String apiKey, String baseURI, Vertx vertx) {
    super(customerId, apiKey, baseURI, vertx);
  }

  public CompletableFuture<VendorResult> retrieveProvider(long id, String include) {

    CompletableFuture<VendorById> vendorFuture;
    CompletableFuture<Packages> packagesFuture;

    vendorFuture = super.retrieveProvider(id);
    if (INCLUDE_PACKAGES_VALUE.equalsIgnoreCase(include)) {
      packagesFuture = retrievePackages(id);
    } else {
      packagesFuture = completedFuture(null);
    }
    return CompletableFuture.allOf(vendorFuture, packagesFuture)
      .thenCompose(o ->
        completedFuture(new VendorResult(vendorFuture.join(), packagesFuture.join())));
  }


  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes) {
    CompletableFuture<Title> titleFuture;
    CompletableFuture<PackageByIdData> packageFuture;
    CompletableFuture<VendorResult> vendorFuture;

    titleFuture = super.retrieveResource(resourceId);
    if (includes.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = retrieveProvider(resourceId.getProviderIdPart(), "");
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }
    if (includes.contains(INCLUDE_PACKAGE_VALUE)) {
      PackageId id = PackageId.builder()
        .providerIdPart(resourceId.getProviderIdPart())
        .packageIdPart(resourceId.getPackageIdPart()).build();
      packageFuture = retrievePackage(id);
    } else {
      packageFuture = completedFuture(null);
    }
    boolean includeTitle = includes.contains("title");

    return CompletableFuture.allOf(titleFuture, vendorFuture, packageFuture)
      .thenCompose(o ->
        completedFuture(new ResourceResult(titleFuture.join(), vendorFuture.join().getVendor(), packageFuture.join(), includeTitle)));
  }


  public CompletableFuture<PackageResult> retrievePackage(PackageId packageId, List<String> includedObjects) {
    CompletableFuture<PackageByIdData> packageFuture = retrievePackage(packageId);

    CompletableFuture<Titles> titlesFuture;
    if (includedObjects.contains(INCLUDE_RESOURCES_VALUE)) {
      titlesFuture = retrieveTitles(packageId.getProviderIdPart(), packageId.getPackageIdPart(), FilterQuery.builder().build(),
        Sort.NAME, 1, 25);
    } else {
      titlesFuture = completedFuture(null);
    }

    CompletableFuture<VendorResult> vendorFuture;
    if (includedObjects.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = retrieveProvider(packageId.getProviderIdPart(), null);
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }

    return CompletableFuture.allOf(packageFuture, titlesFuture, vendorFuture)
      .thenCompose(o ->
        completedFuture(new PackageResult(packageFuture.join(), vendorFuture.join().getVendor(), titlesFuture.join())));
  }

}
