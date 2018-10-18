package org.folio.rest.model;
import org.folio.rest.util.HeaderConstants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Parses and stores data from okapi headers
 */
public class OkapiData {

  private String apiToken;
  private String tenant;
  private String okapiHost;
  private int okapiPort;

  public OkapiData(Map<String, String> headers) {
    try {
      apiToken = headers.get(HeaderConstants.OKAPI_TOKEN_HEADER);
      tenant = headers.get(HeaderConstants.OKAPI_TENANT_HEADER);
      URL url = new URL(headers.get(HeaderConstants.OKAPI_URL_HEADER));
      okapiHost = url.getHost();
      okapiPort = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Okapi url header does not contain valid url");
    }
  }

  public String getApiToken() {
    return apiToken;
  }

  public String getTenant() {
    return tenant;
  }

  public String getOkapiHost() {
    return okapiHost;
  }

  public int getOkapiPort() {
    return okapiPort;
  }
}

