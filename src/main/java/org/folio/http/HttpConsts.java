package org.folio.http;

import org.apache.http.protocol.HTTP;

public final class HttpConsts {

  public static final String CONTENT_TYPE_HEADER = HTTP.CONTENT_TYPE;
  public static final String JSON_API_TYPE = "application/vnd.api+json";

  private HttpConsts() {
  }
}
