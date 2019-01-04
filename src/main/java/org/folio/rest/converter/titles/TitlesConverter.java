package org.folio.rest.converter.titles;

import static org.folio.rest.converter.titles.TitleConverterUtils.createEmptyResourcesRelationships;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.TitleListDataAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.jaxrs.model.Titles;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.Subject;
import org.folio.rmapi.model.Title;

@Component
public class TitlesConverter implements Converter<Title, org.folio.rest.jaxrs.model.Titles> {

  @Autowired
  private Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
  @Autowired
  private Converter<List<Subject>, List<TitleSubject>> subjectsConverter;

  @Override
  public Titles convert(@NonNull Title title) {
    return new org.folio.rest.jaxrs.model.Titles()
      .withId(String.valueOf(title.getTitleId()))
      .withRelationships(createEmptyResourcesRelationships())
      .withType(TITLES_TYPE)
      .withAttributes(new TitleListDataAttributes()
        .withName(title.getTitleName())
        .withPublisherName(title.getPublisherName())
        .withIsTitleCustom(title.getIsTitleCustom())
        .withPublicationType(ConverterConsts.publicationTypes.get(title.getPubType().toLowerCase()))
        .withSubjects(subjectsConverter.convert(title.getSubjectsList()))
        .withIdentifiers(identifiersConverter.convert(title.getIdentifiersList())));

  }
  
}
