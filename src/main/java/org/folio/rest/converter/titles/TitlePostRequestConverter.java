package org.folio.rest.converter.titles;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.TitlePost;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rmapi.result.ResourceResult;

@Component
public class TitlePostRequestConverter implements Converter<TitlePostRequest, TitlePost> {

  @Autowired
  private Converter<ResourceResult, List<Resource>> resourcesConverter;
  @Autowired
  private Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter;
  @Autowired
  private Converter<List<Contributors>, List<Contributor>> toContributorsConverter;


  @Override
  public TitlePost convert(@NonNull TitlePostRequest entity) {
    Boolean isPeerReviewed = entity.getData().getAttributes().getIsPeerReviewed();
    TitlePost.TitlePostBuilder titlePost = TitlePost.builder()
      .titleName(entity.getData().getAttributes().getName())
      .description(entity.getData().getAttributes().getDescription())
      .edition(entity.getData().getAttributes().getEdition())
      .isPeerReviewed(java.util.Objects.isNull(isPeerReviewed) ? Boolean.FALSE : isPeerReviewed)
      .publisherName(entity.getData().getAttributes().getPublisherName())
      .pubType(ConverterConsts.publicationTypes.inverseBidiMap().get(entity.getData().getAttributes().getPublicationType()));

    List<org.folio.rest.jaxrs.model.Identifier> identifiersList = entity.getData().getAttributes().getIdentifiers();
    if (!identifiersList.isEmpty()) {
      titlePost.identifiersList(toIdentifiersConverter.convert(identifiersList));
    }

    List<org.folio.rest.jaxrs.model.Contributors> contributorsList = entity.getData().getAttributes().getContributors();
    if (!contributorsList.isEmpty()) {
      titlePost.contributorsList(toContributorsConverter.convert(contributorsList));
    }

    return titlePost.build();
  }
}
