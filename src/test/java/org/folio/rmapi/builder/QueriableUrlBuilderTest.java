package org.folio.rmapi.builder;

import org.folio.rest.model.Sort;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueriableUrlBuilderTest {
  @Test
  public void shouldBuildUrlForNameSortWhenSortName(){
    String path = new QueriableUrlBuilder().q("ebsco").nameParameter("vendorname").sort(Sort.NAME).build();
    assertEquals("search=ebsco&offset=1&count=25&orderby=vendorname", path);
  }

  @Test
  public void shouldBuildUrlForRelevanceSortWhenSortRelevance(){
    String path = new QueriableUrlBuilder().sort(Sort.RELEVANCE).q("ebsco").build();
    assertEquals("search=ebsco&offset=1&count=25&orderby=relevance", path);
  }

  @Test
  public void shouldBuildUrlForNameSortWhenQueryIsNotSet(){
    String path = new QueriableUrlBuilder().nameParameter("vendorname").build();
    assertEquals("search=&offset=1&count=25&orderby=vendorname", path);
  }

}
