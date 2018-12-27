package org.folio.rest.converter.titles;

import static org.folio.rest.converter.titles.TitleRequestConverter.createEmptyResourcesRelationships;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import org.folio.rest.converter.util.CommonAttributesConverter;
import org.folio.rest.jaxrs.model.TitleListDataAttributes;
import org.folio.rest.jaxrs.model.Titles;
import org.folio.rmapi.model.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitlesConverter implements Converter<Title, org.folio.rest.jaxrs.model.Titles> {
  @Autowired
  private CommonAttributesConverter commonConverter;

  @Override
  public Titles convert(Title title) {
    return new org.folio.rest.jaxrs.model.Titles()
      .withId(String.valueOf(title.getTitleId()))
      .withRelationships(createEmptyResourcesRelationships())
      .withType(TITLES_TYPE)
      .withAttributes(new TitleListDataAttributes()
        .withName(title.getTitleName())
        .withPublisherName(title.getPublisherName())
        .withIsTitleCustom(title.getIsTitleCustom())
        .withPublicationType(CommonAttributesConverter.publicationTypes.get(title.getPubType().toLowerCase()))
        .withSubjects(commonConverter.convertSubjects(title.getSubjectsList()))
        .withIdentifiers(commonConverter.convertIdentifiers(title.getIdentifiersList())));

  }
}
