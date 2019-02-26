package org.folio.rest.util;

import org.folio.rest.jaxrs.model.JsonAPI;

public final class RestConstants {
  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String OKAPI_TENANT_HEADER = "x-okapi-tenant";
  public static final JsonAPI JSONAPI = new JsonAPI().withVersion("1.0");
  public static final String PACKAGES_TYPE = "packages";
  public static final String PROVIDERS_TYPE = "providers";
  public static final String TITLES_TYPE = "titles";
  public static final String RESOURCES_TYPE = "resources";
  public static final String JSON_API_TYPE = "application/vnd.api+json";

  private RestConstants(){ }
}
