package org.folio.rest.converter.titles;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.TitlePost;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
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
    TitlePostDataAttributes attributes = entity.getData().getAttributes();
    Boolean isPeerReviewed = attributes.getIsPeerReviewed();
    TitlePost.TitlePostBuilder titlePost = TitlePost.builder()
      .titleName(attributes.getName())
      .description(attributes.getDescription())
      .edition(attributes.getEdition())
      .isPeerReviewed(java.util.Objects.isNull(isPeerReviewed) ? Boolean.FALSE : isPeerReviewed)
      .publisherName(attributes.getPublisherName())
      .pubType(ConverterConsts.publicationTypes.inverseBidiMap().get(attributes.getPublicationType()))
      .userDefinedFields(UserDefinedFields.builder()
        .userDefinedField1(attributes.getUserDefinedField1())
        .userDefinedField2(attributes.getUserDefinedField2())
        .userDefinedField3(attributes.getUserDefinedField3())
        .userDefinedField4(attributes.getUserDefinedField4())
        .userDefinedField5(attributes.getUserDefinedField5())
          .build()
      );

    List<org.folio.rest.jaxrs.model.Identifier> identifiersList = attributes.getIdentifiers();
    if (!identifiersList.isEmpty()) {
      titlePost.identifiersList(toIdentifiersConverter.convert(identifiersList));
    }

    List<org.folio.rest.jaxrs.model.Contributors> contributorsList = attributes.getContributors();
    if (!contributorsList.isEmpty()) {
      titlePost.contributorsList(toContributorsConverter.convert(contributorsList));
    }

    return titlePost.build();
  }
}
