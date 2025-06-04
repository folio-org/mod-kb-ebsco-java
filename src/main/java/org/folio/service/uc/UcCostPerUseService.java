package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.Order;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.TitleCostPerUse;

public interface UcCostPerUseService {

  CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, String fiscalYear,
                                                              Map<String, String> okapiHeaders);

  CompletableFuture<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                        Map<String, String> okapiHeaders);

  CompletableFuture<PackageCostPerUse> getPackageCostPerUse(String packageId, String platform, String fiscalYear,
                                                            Map<String, String> okapiHeaders);

  CompletableFuture<ResourceCostPerUseCollection> getPackageResourcesCostPerUse(String packageId, String platform,
                                                                                String fiscalYear, String sort,
                                                                                Order order,
                                                                                int page, int size,
                                                                                Map<String, String> okapiHeaders);

  CompletableFuture<ResourceCostPerUseCollection> getPackageResourcesCostPerUse(String packageId, String platform,
                                                                                String fiscalYear,
                                                                                Map<String, String> okapiHeaders);
}
