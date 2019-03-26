package org.folio.rest.util;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

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
  public static final String TAGS_TYPE = "tags";
  public static final String JSON_API_TYPE = "application/vnd.api+json";

  public static final String PROVIDER_RECTYPE = "provider";
  public static final String PACKAGE_RECTYPE = "package";
  public static final String TITLE_RECTYPE = "title";
  public static final String RESOURCE_RECTYPE = "resource";

  public static final Map<String, String> FILTER_SELECTED_MAPPING =
    ImmutableMap.of(
      "true", "selected",
      "false", "notselected",
      "ebsco", "orderedthroughebsco"
    );

  private RestConstants(){ }
}
