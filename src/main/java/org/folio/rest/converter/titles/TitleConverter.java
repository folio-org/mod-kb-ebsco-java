package org.folio.rest.converter.titles;

import static org.folio.rest.converter.titles.TitleConverterUtils.createEmptyResourcesRelationships;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.Resources;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Contributor;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.Subject;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.TitleResult;

@Component
public class TitleConverter implements Converter<TitleResult, Title> {

  private static final String RESOURCES_TYPE = "resources";

  @Autowired
  private Converter<ResourceResult, List<Resource>> resourcesConverter;
  @Autowired
  private Converter<List<Contributor>, List<Contributors>> contributorsConverter;
  @Autowired
  private Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
  @Autowired
  private Converter<List<Subject>, List<TitleSubject>> subjectsConverter;

  @Override
  public org.folio.rest.jaxrs.model.Title convert(@NonNull TitleResult titleResult) {
    org.folio.rmapi.model.Title rmapiTitle = titleResult.getTitle();
    boolean include = titleResult.isIncludeResource();

    List<CustomerResources> customerResourcesList = rmapiTitle.getCustomerResourcesList();
    org.folio.rest.jaxrs.model.Title title = new org.folio.rest.jaxrs.model.Title()
      .withData(new Data()
        .withId(String.valueOf(rmapiTitle.getTitleId()))
        .withType(TITLES_TYPE)
        .withAttributes(new TitleAttributes()
          .withName(rmapiTitle.getTitleName())
          .withPublisherName(rmapiTitle.getPublisherName())
          .withIsTitleCustom(rmapiTitle.getIsTitleCustom())
          .withPublicationType(ConverterConsts.publicationTypes.get(rmapiTitle.getPubType().toLowerCase()))
          .withSubjects(subjectsConverter.convert(rmapiTitle.getSubjectsList()))
          .withIdentifiers(identifiersConverter.convert(rmapiTitle.getIdentifiersList()))
          .withEdition(rmapiTitle.getEdition())
          .withContributors(contributorsConverter.convert(rmapiTitle.getContributorsList()))
          .withDescription(rmapiTitle.getDescription())
          .withIsPeerReviewed(rmapiTitle.getIsPeerReviewed())
        )
        .withRelationships(createEmptyResourcesRelationships())
      )
      .withIncluded(null)
      .withJsonapi(RestConstants.JSONAPI);
    if (include && Objects.nonNull(customerResourcesList)) {
      title
        .withIncluded(resourcesConverter.convert(new ResourceResult(rmapiTitle, null, null, false))
          .stream()
          .map(Resource::getData)
          .collect(Collectors.toList())).getData()
        .withRelationships(new Relationships().withResources(new Resources()
          .withData(convertResourcesRelationship(customerResourcesList))));
    }
    return title;
  }

  private List<RelationshipData> convertResourcesRelationship(List<CustomerResources> customerResources) {
    return customerResources.stream()
      .map(resourceData ->
        new RelationshipData()
          .withId(resourceData.getVendorId() + "-" + resourceData.getPackageId() + "-" + resourceData.getTitleId())
          .withType(RESOURCES_TYPE))
      .collect(Collectors.toList());
  }
}
