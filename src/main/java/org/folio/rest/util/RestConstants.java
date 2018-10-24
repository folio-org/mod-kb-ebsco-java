package org.folio.rest.util;

import org.folio.rest.jaxrs.model.JsonAPI;

public final class RestConstants {
  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String OKAPI_TENANT_HEADER = "x-okapi-tenant";
  public static final JsonAPI JSONAPI = new JsonAPI().withVersion("1.0");
  private RestConstants(){ }
}
