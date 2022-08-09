package org.folio.client.uc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.client.uc.configuration.GetPackageUcConfiguration;
import org.folio.client.uc.configuration.GetTitlePackageUcConfiguration;
import org.folio.client.uc.configuration.GetTitleUcConfiguration;
import org.folio.client.uc.configuration.UcConfiguration;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcMetricType;
import org.folio.client.uc.model.UcPackageCostPerUse;
import org.folio.client.uc.model.UcTitleCostPerUse;
import org.folio.client.uc.model.UcTitlePackageId;

public interface UcApigeeEbscoClient {

  CompletableFuture<Boolean> verifyCredentials(UcConfiguration configuration);

  CompletableFuture<UcTitleCostPerUse> getTitleCostPerUse(String titleId, String packageId,
                                                          GetTitleUcConfiguration configuration);

  CompletableFuture<UcPackageCostPerUse> getPackageCostPerUse(String packageId,
                                                              GetPackageUcConfiguration configuration);

  CompletableFuture<Map<String, UcCostAnalysis>> getTitlePackageCostPerUse(
    List<UcTitlePackageId> ids,
    GetTitlePackageUcConfiguration configuration);

  CompletableFuture<UcMetricType> getUsageMetricType(UcConfiguration configuration);
}
