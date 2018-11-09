package org.folio.rmapi.builder;

import com.google.common.collect.ImmutableMap;
import org.folio.rest.model.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TitlesFilterableUrlBuilder {
  private static final Map<String, String> FILTER_SELECTED_MAPPING =
    ImmutableMap.of(
      "true", "selected",
      "false", "notselected",
      "ebsco", "orderedthroughebsco"
    );

  private String filterSelected;
  private String filterType;
  private String filterName;
  private String filterIsxn;
  private String filterSubject;
  private String filterPublisher;
  private int page = 1;
  private int count = 25;
  private Sort sort;

  public TitlesFilterableUrlBuilder filterSelected(String filterSelected) {
    this.filterSelected = filterSelected;
    return this;
  }

  public TitlesFilterableUrlBuilder filterType(String filterType) {
    this.filterType = filterType;
    return this;
  }

  public TitlesFilterableUrlBuilder filterName(String filterName) {
    this.filterName = filterName;
    return this;
  }

  public TitlesFilterableUrlBuilder filterIsxn(String filterIsxn) {
    this.filterIsxn = filterIsxn;
    return this;
  }

  public TitlesFilterableUrlBuilder filterSubject(String filterSubject) {
    this.filterSubject = filterSubject;
    return this;
  }

  public TitlesFilterableUrlBuilder filterPublisher(String filterPublisher) {
    this.filterPublisher = filterPublisher;
    return this;
  }

  public TitlesFilterableUrlBuilder page(int page) {
    this.page = page;
    return this;
  }

  public TitlesFilterableUrlBuilder count(int count) {
    this.count = count;
    return this;
  }

  public TitlesFilterableUrlBuilder sort(Sort sort) {
    this.sort = sort;
    return this;
  }

  public String build(){
    String search = null;
    String searchField = null;
    if (filterName != null) {
      search = filterName;
      searchField = "titlename";
    } else if (filterIsxn != null) {
      search = filterIsxn;
      searchField = "isxn";
    } else if (filterSubject != null) {
      search = filterSubject;
      searchField = "subject";
    } else if (filterPublisher != null) {
      search = filterPublisher;
      searchField = "publisher";
    }

    String selection = FILTER_SELECTED_MAPPING.getOrDefault(filterSelected, "all");

    String resourceType = filterType != null ? filterType : "all";

    List<String> parameters = new ArrayList<>();
    if(searchField != null){
      parameters.add("searchfield="+searchField);
    }
    parameters.add("selection=" + selection);
    parameters.add("resourcetype=" + resourceType);
    parameters.add("searchtype=advanced");

    String query = new QueriableUrlBuilder()
      .q(search)
      .page(page)
      .count(count)
      .sort(sort)
      .nameParameter("titlename")
      .build();

    parameters.add(query);
    return  String.join("&", parameters);
  }
}
