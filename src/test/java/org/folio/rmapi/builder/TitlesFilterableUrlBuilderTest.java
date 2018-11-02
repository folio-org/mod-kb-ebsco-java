package org.folio.rmapi.builder;

import org.folio.rest.model.Sort;
import org.junit.Test;

import static org.junit.Assert.*;

public class TitlesFilterableUrlBuilderTest {
  @Test
  public void shouldBuildUrlWithCount(){
    String url = new TitlesFilterableUrlBuilder()
      .filterName("ebsco")
      .count(5)
      .build();
    assertEquals("searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=ebsco" +
      "&offset=1&count=5&orderby=relevance", url);
  }

  @Test
  public void shouldBuildUrlWithSort(){
    String url = new TitlesFilterableUrlBuilder()
      .filterName("ebsco")
      .sort(Sort.NAME)
      .build();
    assertEquals("searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=ebsco" +
      "&offset=1&count=25&orderby=titlename", url);
  }

  @Test
  public void shouldBuildUrlWithPage(){
    String url = new TitlesFilterableUrlBuilder()
      .filterName("ebsco")
      .page(2)
      .build();
    assertEquals("searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=ebsco" +
      "&offset=2&count=25&orderby=relevance", url);
  }

  @Test
  public void shouldBuildUrlForFilterBySelectedStatus(){
    String url = new TitlesFilterableUrlBuilder()
      .filterName("news")
      .filterType("book")
      .filterSelected("true")
      .build();
    assertEquals("searchfield=titlename&selection=selected&resourcetype=book&searchtype=advanced&search=news" +
      "&offset=1&count=25&orderby=relevance", url);
  }

  @Test
  public void shouldBuildUrlForFilterByIsxn(){
    String url = new TitlesFilterableUrlBuilder()
      .filterIsxn("1362-3613")
      .build();
    assertEquals("searchfield=isxn&selection=all&resourcetype=all&searchtype=advanced&search=1362-3613" +
      "&offset=1&count=25&orderby=relevance", url);
  }

  @Test
  public void shouldBuildUrlForFilterBySubject(){
    String url = new TitlesFilterableUrlBuilder()
      .filterSubject("history")
      .build();
    assertEquals("searchfield=subject&selection=all&resourcetype=all&searchtype=advanced&search=history" +
      "&offset=1&count=25&orderby=relevance", url);
  }

  @Test
  public void shouldBuildUrlForFilterByPublisher(){
    String url = new TitlesFilterableUrlBuilder()
      .filterPublisher("publisherName")
      .build();
    assertEquals("searchfield=publisher&selection=all&resourcetype=all&searchtype=advanced&search=publisherName" +
      "&offset=1&count=25&orderby=relevance", url);
  }

}

