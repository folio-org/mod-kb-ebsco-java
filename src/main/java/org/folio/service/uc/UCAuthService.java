package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface UCAuthService {

  CompletableFuture<String> authenticate(Map<String, String> okapiHeaders);
}
