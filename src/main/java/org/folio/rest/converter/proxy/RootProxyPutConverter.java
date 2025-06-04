package org.folio.rest.converter.proxy;

import org.folio.holdingsiq.model.Proxy;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.springframework.stereotype.Component;

@Component
public class RootProxyPutConverter {

  public RootProxyCustomLabels convertToRootProxyCustomLabels(RootProxyPutRequest rootProxyPutRequest,
                                                              RootProxyCustomLabels rootProxyCustomLabels) {
    /*
     * In RM API - custom labels and root proxy are updated using the same PUT endpoint.
     * We are GETting the object containing both, updating the root proxy with the new one and making a PUT
     * request to RM API.
     * Custom Labels contain only values that have display labels up to a maximum of 5 with fewer possible
     */
    return rootProxyCustomLabels.toBuilder()
      .proxy(Proxy.builder().id(rootProxyPutRequest.getData().getAttributes().getProxyTypeId()).build())
      .labelList(rootProxyCustomLabels.getLabelList()).build();
  }
}
