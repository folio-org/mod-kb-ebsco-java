package org.folio.rest.util;

import java.util.List;
import java.util.Map;
import org.folio.holdingsiq.model.PackageFilterFreeAccess;
import org.folio.holdingsiq.model.PackageFilterVisibility;
import org.folio.rest.jaxrs.model.JsonAPI;

public final class RestConstants {

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
    Map.of(
      "true", "selected",
      "false", "notselected",
      "ebsco", "orderedthroughebsco"
    );

  public static final List<String> SUPPORTED_PACKAGE_FILTER_TYPE_VALUES =
    List.of("all", "aggregatedfulltext", "abstractandindex", "ebook", "ejournal", "print", "unknown",
      "onlinereference", "streamingmedia", "mixedcontent");

  public static final Map<String, PackageFilterVisibility> FILTER_VISIBILITY_MAPPING =
    Map.of("visibleInPF", PackageFilterVisibility.VISIBLE_IN_PF,
      "visibleInFTF", PackageFilterVisibility.VISIBLE_IN_FTF,
      "visibleInMARC", PackageFilterVisibility.INCLUDED_IN_MARC,
      "hiddenInPF", PackageFilterVisibility.HIDDEN_IN_PF,
      "hiddenInFTF", PackageFilterVisibility.HIDDEN_IN_FTF,
      "hiddenInMARC", PackageFilterVisibility.EXCLUDED_FROM_MARC);

  public static final Map<String, PackageFilterFreeAccess> FILTER_FREE_ACCESS_MAPPING =
    Map.of("true", PackageFilterFreeAccess.TRUE,
      "false", PackageFilterFreeAccess.FALSE);

  public static final List<String> SUPPORTED_TITLE_FILTER_TYPE_VALUES =
    List.of("audiobook", "book", "bookseries", "database", "journal", "newsletter", "newspaper", "proceedings",
      "report",
      "streamingaudio", "streamingvideo", "thesisdissertation", "website", "unspecified");

  private RestConstants() {
  }
}
