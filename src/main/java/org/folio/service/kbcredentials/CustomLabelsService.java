package org.folio.service.kbcredentials;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Configuration;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;

public interface CustomLabelsService {

  CompletableFuture<CustomLabelsCollection> fetchCustomLabels(Configuration configuration, Map<String, String> okapiHeaders);
}
