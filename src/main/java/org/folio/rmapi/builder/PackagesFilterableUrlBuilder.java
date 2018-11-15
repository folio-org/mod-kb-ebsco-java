package org.folio.rmapi.builder;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.folio.rest.model.Sort;

public class PackagesFilterableUrlBuilder {

  private static final Map<String, String> FILTER_SELECTED_MAPPING =
    ImmutableMap.of(
      "true", "selected",
      "false", "notselected",
      "ebsco", "orderedthroughebsco"
    );

  private String filterSelected;
  private String filterType;
  private int page = 1;
  private int count = 25;
  private Sort sort;
  private String q;

  public PackagesFilterableUrlBuilder filterSelected(String filterSelected) {
    this.filterSelected = filterSelected;
    return this;
  }

  public PackagesFilterableUrlBuilder filterType(String filterType) {
    this.filterType = filterType;
    return this;
  }

  public PackagesFilterableUrlBuilder page(int page) {
    this.page = page;
    return this;
  }

  public PackagesFilterableUrlBuilder count(int count) {
    this.count = count;
    return this;
  }

  public PackagesFilterableUrlBuilder sort(Sort sort) {
    this.sort = sort;
    return this;
  }

  public PackagesFilterableUrlBuilder q(String q) {
    this.q = q;
    return this;
  }

  public String build(){

    String selection = FILTER_SELECTED_MAPPING.getOrDefault(filterSelected, "all");
    String contentType = StringUtils.defaultIfEmpty(filterType, "all");
    List<String> parameters = new ArrayList<>();

    parameters.add("selection=" + selection);
    parameters.add("contenttype=" + contentType);
    String query = new QueriableUrlBuilder()
      .q(q)
      .page(page)
      .count(count)
      .sort(sort)
      .nameParameter("packagename")
      .build();
    parameters.add(query);
    return  String.join("&", parameters);
  }
}
