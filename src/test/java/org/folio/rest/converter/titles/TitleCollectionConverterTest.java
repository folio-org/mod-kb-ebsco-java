package org.folio.rest.converter.titles;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.PackageFacet;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.jaxrs.model.FacetsDto;
import org.folio.rest.jaxrs.model.PackageFacetDto;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleCollectionItem;
import org.folio.rest.jaxrs.model.TitleCollectionItemDataAttributes;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

public class TitleCollectionConverterTest {

  private final Converter<Title, TitleCollectionItem> titleItemConverterMock = title -> new TitleCollectionItem()
    .withId(String.valueOf(title.getTitleId()))
    .withAttributes(new TitleCollectionItemDataAttributes()
      .withName(title.getTitleName()));

  private final Converter<Facets, FacetsDto> facetsConverterMock = facets -> new FacetsDto()
    .withPackages(singletonList(new PackageFacetDto()
      .withId(facets.getPackages().get(0).getPackageId())
      .withName(facets.getPackages().get(0).getPackageName())));
  private final TitleCollectionConverter.FromTitles converter = new TitleCollectionConverter.FromTitles(
    titleItemConverterMock, facetsConverterMock);

  @Test
  public void shouldConvertFromTitles() {
    Titles titles = createTitles();

    TitleCollection titlesCollection = converter.convert(titles);

    assertEquals(titles.getTotalResults(), titlesCollection.getMeta().getTotalResults());
    assertEquals(String.valueOf(titles.getTitleList().get(0).getTitleId()), titlesCollection.getData().get(0).getId());
    assertEquals(titles.getTitleList().get(0).getTitleName(), titlesCollection.getData().get(0).getAttributes().getName());
    assertEquals(titles.getFacets().getPackages().get(0).getPackageId(), titlesCollection.getFacets().getPackages().get(0).getId());
    assertEquals(titles.getFacets().getPackages().get(0).getPackageName(), titlesCollection.getFacets().getPackages().get(0).getName());
  }

  @Test
  public void shouldConvertFromTitlesWithNullFacets() {
    Titles titles = createTitles(null);

    TitleCollection titlesCollection = converter.convert(titles);

    assertEquals(titles.getTotalResults(), titlesCollection.getMeta().getTotalResults());
    assertNull(titles.getFacets());
    assertNotNull(titles.getTitleList());
  }

  private Titles createTitles() {
    Facets facets = Facets.builder()
      .packages(singletonList(PackageFacet.builder()
        .packageId(2)
        .packageName("test")
        .build()))
      .build();

    return createTitles(facets);
  }

  private Titles createTitles(Facets facets) {
    List<Title> titleList = singletonList(Title.builder()
      .titleId(1)
      .titleName("test")
      .build());

    return Titles.builder()
      .titleList(titleList)
      .facets(facets)
      .totalResults(3)
      .build();
  }
}

