package org.folio.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleListDataAttributes;
import org.folio.rest.jaxrs.model.TitleRelationship;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;

public class TitleConverter {
  
  private CommonAttributesConverter commonConverter;

  private static final TitleRelationship EMPTY_RESOURCES_RELATIONSHIP = new TitleRelationship()
    .withResources(new MetaIncluded().withMeta(
      new MetaDataIncluded()
        .withIncluded(false)));

  public TitleConverter() {
    this(new CommonAttributesConverter());
  }
  
  public TitleConverter(CommonAttributesConverter commonConverter) {
    this.commonConverter = commonConverter;
  }

  public TitleCollection convert(Titles titles) {
    List<org.folio.rest.jaxrs.model.Titles> titleList = titles.getTitleList().stream()
      .map(this::convertTitle)
      .collect(Collectors.toList());
    return new TitleCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(titleList);
  }

  public org.folio.rest.jaxrs.model.Title convertFromRMAPITitle(org.folio.rmapi.model.Title rmapiTitle) {
    return new org.folio.rest.jaxrs.model.Title()
        .withData(new Data()
            .withId(String.valueOf(rmapiTitle.getTitleId()))
            .withType("titles")
            .withAttributes(new TitleAttributes()
                .withName(rmapiTitle.getTitleName())
                .withPublisherName(rmapiTitle.getPublisherName())
                .withIsTitleCustom(rmapiTitle.getTitleCustom())
                .withPublicationType(CommonAttributesConverter.publicationTypes.get(rmapiTitle.getPubType().toLowerCase()))
                .withSubjects(commonConverter.convertSubjects(rmapiTitle.getSubjectsList()))
                .withIdentifiers(commonConverter.convertIdentifiers(rmapiTitle.getIdentifiersList()))
                .withEdition(rmapiTitle.getEdition())
                .withContributors(commonConverter.convertContributors(rmapiTitle.getContributorsList()))
                .withDescription(rmapiTitle.getDescription())
                .withIsPeerReviewed(rmapiTitle.getPeerReviewed())
                )
            .withRelationships(EMPTY_RESOURCES_RELATIONSHIP)
            )
        .withIncluded(null)
        .withJsonapi(RestConstants.JSONAPI);
  }
  
  private org.folio.rest.jaxrs.model.Titles convertTitle(Title title) {
    return new org.folio.rest.jaxrs.model.Titles()
      .withId(String.valueOf(title.getTitleId()))
      .withRelationships(EMPTY_RESOURCES_RELATIONSHIP)
      .withType("titles")
      .withAttributes(new TitleListDataAttributes()
        .withName(title.getTitleName())
        .withPublisherName(title.getPublisherName())
        .withIsTitleCustom(title.getTitleCustom())
        .withPublicationType(CommonAttributesConverter.publicationTypes.get(title.getPubType().toLowerCase()))
        .withSubjects(commonConverter.convertSubjects(title.getSubjectsList()))
        .withIdentifiers(commonConverter.convertIdentifiers(title.getIdentifiersList())));
  }
}
