package org.folio.rest.converter.titles;

import java.util.List;
import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.TitlePost;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitlePostRequestConverter implements Converter<TitlePostRequest, TitlePost> {

  private final Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter;
  private final Converter<List<Contributors>, List<Contributor>> toContributorsConverter;

  public TitlePostRequestConverter(
    Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter,
    Converter<List<Contributors>, List<Contributor>> toContributorsConverter) {
    this.toIdentifiersConverter = toIdentifiersConverter;
    this.toContributorsConverter = toContributorsConverter;
  }

  @Override
  public TitlePost convert(TitlePostRequest entity) {
    TitlePostDataAttributes attributes = entity.getData().getAttributes();
    Boolean isPeerReviewed = attributes.getIsPeerReviewed();
    TitlePost.TitlePostBuilder titlePost = TitlePost.builder()
      .titleName(attributes.getName())
      .description(attributes.getDescription())
      .edition(attributes.getEdition())
      .isPeerReviewed(java.util.Objects.isNull(isPeerReviewed) ? Boolean.FALSE : isPeerReviewed)
      .publisherName(attributes.getPublisherName())
      .pubType(ConverterConsts.PUBLICATION_TYPES.inverseBidiMap().get(attributes.getPublicationType()))
      .userDefinedFields(convertUserDefinedFields(attributes)
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

  private UserDefinedFields convertUserDefinedFields(TitlePostDataAttributes attributes) {
    return UserDefinedFields.builder()
      .userDefinedField1(attributes.getUserDefinedField1())
      .userDefinedField2(attributes.getUserDefinedField2())
      .userDefinedField3(attributes.getUserDefinedField3())
      .userDefinedField4(attributes.getUserDefinedField4())
      .userDefinedField5(attributes.getUserDefinedField5())
      .build();
  }
}
