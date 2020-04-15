package org.folio.service.customlabels;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.CustomLabelsCollection;

public interface CustomLabelsService {

  CompletableFuture<CustomLabelsCollection> fetchCustomLabels(String credentialsId, Map<String, String> okapiHeaders);
}
