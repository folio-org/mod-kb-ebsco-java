package org.folio.rmapi.builder;

import com.google.common.base.Strings;
import org.folio.rest.model.Sort;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class QueriableUrlBuilder {
  private static final String RELEVANCE_PARAMETER = "relevance";
  private String q;
  private int page = 1;
  private int count = 25;
  private Sort sort;
  private String nameParameter;

  public QueriableUrlBuilder q(String q) {
    this.q = q;
    return this;
  }

  public QueriableUrlBuilder page(int page) {
    this.page = page;
    return this;
  }

  public QueriableUrlBuilder count(int count) {
    this.count = count;
    return this;
  }

  public QueriableUrlBuilder sort(Sort sort) {
    this.sort = sort;
    return this;
  }

  public QueriableUrlBuilder nameParameter(String nameParameter) {
    this.nameParameter = nameParameter;
    return this;
  }

  public String build(){
    List<String> parameters = new ArrayList<>();
    if (!Strings.isNullOrEmpty(q)) {
      String encodedQuery;
      try {
        encodedQuery = URLEncoder.encode(q, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("failed to encode query using UTF-8");
      }
      parameters.add("search=" + encodedQuery);
    }
    parameters.add("offset=" + page);
    parameters.add("count=" + count);
    parameters.add("orderby=" + determineSortValue(sort, q));

    return String.join("&", parameters);
  }

  private String determineSortValue(Sort sort, String query) {
    if(sort == null){
      return query == null ? nameParameter : RELEVANCE_PARAMETER;
    }
    switch (sort){
      case RELEVANCE:
        return RELEVANCE_PARAMETER;
      case NAME:
        return nameParameter;
      default:
        throw new IllegalArgumentException("Invalid value for sort - " + sort);
    }
  }
}
