package org.folio.service.uc.export;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ExportService {

  CompletableFuture<String> exportCsv(String packageId, String platform, String year,
                                      Map<String, String> okapiHeaders);
}
