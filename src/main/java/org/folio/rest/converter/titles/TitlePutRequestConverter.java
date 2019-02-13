package org.folio.rest.converter.titles;

import java.util.List;

import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.rmapi.model.Contributor;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.ResourcePut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitlePutRequestConverter {

  private Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter;
  private Converter<List<Contributors>, List<Contributor>> toContributorsConverter;

  @Autowired
  public TitlePutRequestConverter(Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter, Converter<List<Contributors>, List<Contributor>> toContributorsConverter) {
    this.toIdentifiersConverter = toIdentifiersConverter;
    this.toContributorsConverter = toContributorsConverter;
  }

  public ResourcePut convertToRMAPICustomResourcePutRequest(TitlePutRequest entity, CustomerResources oldResource) {
    TitlePostDataAttributes attributes = entity.getData().getAttributes();
    ResourcePut.ResourcePutBuilder builder = ResourcePut.builder();

    builder.proxy(oldResource.getProxy());
    builder.isHidden(oldResource.getVisibilityData().getIsHidden());
    builder.coverageStatement(oldResource.getCoverageStatement());
    builder.customEmbargoPeriod(oldResource.getCustomEmbargoPeriod());
    builder.customCoverageList(oldResource.getCustomCoverageList());
    builder.url(oldResource.getUrl());

    builder.isSelected(true);
    if(attributes.getPublicationType() != null) {
      builder.pubType(ConverterConsts.publicationTypes.inverseBidiMap().get(attributes.getPublicationType()));
    }
    builder.isPeerReviewed(attributes.getIsPeerReviewed());
    builder.titleName(attributes.getName());
    builder.publisherName(attributes.getPublisherName());
    builder.edition(attributes.getEdition());
    builder.description(attributes.getDescription());
    builder.identifiersList(toIdentifiersConverter.convert(attributes.getIdentifiers()));
    builder.contributorsList(toContributorsConverter.convert(attributes.getContributors()));
    return builder.build();
  }
}