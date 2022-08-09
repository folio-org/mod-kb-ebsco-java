package org.folio.service.locale;

import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.OkapiData;

public interface LocaleSettingsService {

  CompletableFuture<LocaleSettings> retrieveSettings(OkapiData okapiData);

}
