package org.folio.rmapi.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.Sort;

public class TitlesFilterableUrlBuilder {
  private static final Map<String, String> FILTER_SELECTED_MAPPING =
    ImmutableMap.of(
      "true", "selected",
      "false", "notselected",
      "ebsco", "orderedthroughebsco"
    );
  private static final String SEARCHFIELD_TITLENAME = "titlename";
  private static final String SEARCHFIELD_ISXN = "isxn";
  private static final String SEARCHFIELD_PUBLISHER = "publisher";
  private static final String SEARCHFIELD_SUBJECT = "subject";

  private FilterQuery filterQuery;
  private int page = 1;
  private int count = 25;
  private Sort sort;

  public TitlesFilterableUrlBuilder filter(FilterQuery filterQuery) {
    this.filterQuery = filterQuery;
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
    if (filterQuery.getName() != null) {
      search = filterQuery.getName();
      searchField = SEARCHFIELD_TITLENAME;
    } else if (filterQuery.getIsxn() != null) {
      search = filterQuery.getIsxn();
      searchField = SEARCHFIELD_ISXN;
    } else if (filterQuery.getSubject() != null) {
      search = filterQuery.getSubject();
      searchField = SEARCHFIELD_SUBJECT;
    } else if (filterQuery.getPublisher() != null) {
      search = filterQuery.getPublisher();
      searchField = SEARCHFIELD_PUBLISHER;
    } else {
      searchField = SEARCHFIELD_TITLENAME;
    }

    String selection = FILTER_SELECTED_MAPPING.getOrDefault(filterQuery.getSelected(), "all");

    String resourceType = StringUtils.defaultString(filterQuery.getType(), "all");

    List<String> parameters = new ArrayList<>();

    parameters.add("searchfield="+searchField);
    parameters.add("selection=" + selection);
    parameters.add("resourcetype=" + resourceType);
    parameters.add("searchtype=advanced");

    String query = new QueriableUrlBuilder()
      .q(search)
      .page(page)
      .count(count)
      .sort(sort)
      .nameParameter(SEARCHFIELD_TITLENAME)
      .build();

    parameters.add(query);
    return  String.join("&", parameters);
  }

}
