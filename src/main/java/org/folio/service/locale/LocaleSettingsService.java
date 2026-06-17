package org.folio.service.locale;

import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.RequestContext;

public interface LocaleSettingsService {

  CompletableFuture<LocaleSettings> retrieveSettings(RequestContext requestContext);
}
