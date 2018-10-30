package org.folio.rmapi.builder;

import com.google.common.base.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class VendorUrlBuilder {

  private static final String VENDOR_NAME_PARAMETER = "vendorname";
  private static final String RELEVANCE_PARAMETER = "relevance";
  private String q;
  private int page = 1;
  private int count = 25;
  private String sort;

  public VendorUrlBuilder q(String q) {
    this.q = q;
    return this;
  }

  public VendorUrlBuilder page(int page) {
    this.page = page;
    return this;
  }

  public VendorUrlBuilder count(int count) {
    this.count = count;
    return this;
  }

  public VendorUrlBuilder sort(String sort) {
    this.sort = sort;
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

    return "vendors?" + String.join("&", parameters);
  }

  private String determineSortValue(String sort, String query) {
    if(sort == null){
      return query == null ? VENDOR_NAME_PARAMETER : RELEVANCE_PARAMETER;
    }
    if(sort.equalsIgnoreCase("relevance")){
      return RELEVANCE_PARAMETER;
    }else if(sort.equalsIgnoreCase("name")){
      return VENDOR_NAME_PARAMETER;
    }
    throw new IllegalArgumentException("Invalid value for sort - " + sort);
  }
}
