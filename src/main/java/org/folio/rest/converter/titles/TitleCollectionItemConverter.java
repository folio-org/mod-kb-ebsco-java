package org.folio.rest.converter.titles;

import static org.folio.rest.converter.titles.TitleConverterUtils.createEmptyResourcesRelationships;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.Subject;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleCollectionItem;
import org.folio.rest.jaxrs.model.TitleCollectionItemDataAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;

public class TitleCollectionItemConverter {

  private TitleCollectionItemConverter() {
  }

  @Component
  public static class FromHoldingsTitle implements Converter<Title, TitleCollectionItem> {

    @Autowired
    private Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
    @Autowired
    private Converter<List<Subject>, List<TitleSubject>> subjectsConverter;
    @Autowired
    private Converter<List<Contributor>, List<Contributors>> contributorsConverter;

    @Override
    public TitleCollectionItem convert(@NonNull Title title) {
      return new TitleCollectionItem()
        .withId(String.valueOf(title.getTitleId()))
        .withType(TitleCollectionItem.Type.TITLES)
        .withAttributes(new TitleCollectionItemDataAttributes()
          .withName(title.getTitleName())
          .withPublisherName(title.getPublisherName())
          .withIsTitleCustom(title.getIsTitleCustom())
          .withPublicationType(ConverterConsts.publicationTypes.get(title.getPubType().toLowerCase()))
          .withSubjects(subjectsConverter.convert(title.getSubjectsList()))
          .withIdentifiers(identifiersConverter.convert(title.getIdentifiersList()))
          .withContributors(contributorsConverter.convert(title.getContributorsList()))
          .withRelationships(createEmptyResourcesRelationships())
        );
    }
  }

  @Component
  public static class FromKbTitles implements Converter<org.folio.rest.jaxrs.model.Title, TitleCollectionItem> {

    @Override
    public TitleCollectionItem convert(org.folio.rest.jaxrs.model.Title source) {
      Data sourceData = source.getData();
      TitleAttributes attributes = sourceData.getAttributes();
      return new TitleCollectionItem()
        .withId(sourceData.getId())
        .withType(TitleCollectionItem.Type.TITLES)
        .withIncluded(source.getIncluded())
        .withAttributes(new TitleCollectionItemDataAttributes()
          .withContributors(attributes.getContributors())
          .withIdentifiers(attributes.getIdentifiers())
          .withIsTitleCustom(attributes.getIsTitleCustom())
          .withName(attributes.getName())
          .withPublicationType(attributes.getPublicationType())
          .withPublisherName(attributes.getPublisherName())
          .withSubjects(attributes.getSubjects())
          .withRelationships(sourceData.getRelationships())
        );
    }
  }
}
