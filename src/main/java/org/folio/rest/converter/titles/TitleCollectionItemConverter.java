package org.folio.rest.converter.titles;

import static org.folio.rest.converter.titles.TitleConverterUtils.createEmptyResourcesRelationships;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.Subject;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.AlternateTitle;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleCollectionItem;
import org.folio.rest.jaxrs.model.TitleCollectionItemDataAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class TitleCollectionItemConverter {

  private TitleCollectionItemConverter() {
  }

  @Component
  public static class FromHoldingsTitle implements Converter<Title, TitleCollectionItem> {

    private final Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
    private final Converter<List<Subject>, List<TitleSubject>> subjectsConverter;
    private final Converter<List<Contributor>, List<Contributors>> contributorsConverter;
    private final Converter<List<org.folio.holdingsiq.model.AlternateTitle>, List<AlternateTitle>>
      alternateTitleConverter;

    public FromHoldingsTitle(
      Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter,
      Converter<List<Subject>, List<TitleSubject>> subjectsConverter,
      Converter<List<Contributor>, List<Contributors>> contributorsConverter,
      Converter<List<org.folio.holdingsiq.model.AlternateTitle>, List<AlternateTitle>> alternateTitleConverter) {
      this.identifiersConverter = identifiersConverter;
      this.subjectsConverter = subjectsConverter;
      this.contributorsConverter = contributorsConverter;
      this.alternateTitleConverter = alternateTitleConverter;
    }

    @Override
    public TitleCollectionItem convert(@NonNull Title title) {
      return new TitleCollectionItem()
        .withId(String.valueOf(title.getTitleId()))
        .withType(TitleCollectionItem.Type.TITLES)
        .withAttributes(new TitleCollectionItemDataAttributes()
          .withName(title.getTitleName())
          .withPublisherName(title.getPublisherName())
          .withIsTitleCustom(title.getIsTitleCustom())
          .withPublicationType(ConverterConsts.PUBLICATION_TYPES.get(title.getPubType().toLowerCase()))
          .withSubjects(subjectsConverter.convert(title.getSubjectsList()))
          .withIdentifiers(identifiersConverter.convert(title.getIdentifiersList()))
          .withContributors(contributorsConverter.convert(title.getContributorsList()))
          .withRelationships(createEmptyResourcesRelationships())
          .withAlternateTitles(alternateTitleConverter.convert(
            Objects.requireNonNullElse(title.getAlternateTitleList(), Collections.emptyList())))
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
          .withAlternateTitles(attributes.getAlternateTitles())
        );
    }
  }
}
