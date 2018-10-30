package org.folio.rmapi.builder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VendorUrlBuilderTest {
  @Test
  public void shouldBuildUrlForNameSortWhenSortName(){
    String path = new VendorUrlBuilder().sort("name").build();
    assertEquals("vendors?offset=1&count=25&orderby=vendorname", path);
  }
  @Test
  public void shouldBuildUrlForRelevanceSortWhenSortRelevance(){
    String path = new VendorUrlBuilder().sort("relevance").build();
    assertEquals("vendors?offset=1&count=25&orderby=relevance", path);
  }

  @Test
  public void shouldBuildUrlForNameSortWhenSortIsNotSet(){
    String path = new VendorUrlBuilder().build();
    assertEquals("vendors?offset=1&count=25&orderby=vendorname", path);
  }

  @Test
  public void shouldBuildUrlForRelevanceSortWhenSortIsNotSetAndQueryIsSet(){
    String path = new VendorUrlBuilder().q("higher education").build();
    assertEquals("vendors?search=higher+education&offset=1&count=25&orderby=relevance", path);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfSortValueIsInvalid() {
    new VendorUrlBuilder().sort("abc").build();
  }
}
