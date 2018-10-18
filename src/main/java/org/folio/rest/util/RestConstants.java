package org.folio.rest.util;

import org.folio.rest.jaxrs.model.Jsonapi;

public final class RestConstants {
  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String OKAPI_TENANT_HEADER = "x-okapi-tenant";
  public static final Jsonapi JSONAPI = new Jsonapi().withVersion("1.0");
  private RestConstants(){ }
}
