package org.folio.service.customlabels;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;

public interface CustomLabelsService {

  CompletableFuture<CustomLabelsCollection> fetch(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<CustomLabelsCollection> update(String credentialsId, CustomLabelsPutRequest putRequest,
                                                   Map<String, String> okapiHeaders);
}
