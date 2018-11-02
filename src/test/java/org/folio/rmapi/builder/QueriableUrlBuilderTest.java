package org.folio.rmapi.builder;

import org.folio.rest.model.Sort;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueriableUrlBuilderTest {
  @Test
  public void shouldBuildUrlForNameSortWhenSortName(){
    String path = new QueriableUrlBuilder().nameParameter("vendorname").sort(Sort.NAME).build();
    assertEquals("offset=1&count=25&orderby=vendorname", path);
  }

  @Test
  public void shouldBuildUrlForRelevanceSortWhenSortRelevance(){
    String path = new QueriableUrlBuilder().sort(Sort.RELEVANCE).build();
    assertEquals("offset=1&count=25&orderby=relevance", path);
  }

  @Test
  public void shouldBuildUrlForNameSortWhenSortIsNotSet(){
    String path = new QueriableUrlBuilder().nameParameter("vendorname").build();
    assertEquals("offset=1&count=25&orderby=vendorname", path);
  }

  @Test
  public void shouldBuildUrlForRelevanceSortWhenSortIsNotSetAndQueryIsSet(){
    String path = new QueriableUrlBuilder().q("higher education").build();
    assertEquals("search=higher+education&offset=1&count=25&orderby=relevance", path);
  }
}
