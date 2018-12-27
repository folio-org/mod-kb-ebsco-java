package org.folio.rest.converter.titles;

import static org.folio.rest.converter.titles.TitleRequestConverter.createEmptyResourcesRelationships;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.rest.converter.util.CommonAttributesConverter;
import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.Resources;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.TitleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitleConverter implements Converter<TitleResult, Title> {

  private static final String RESOURCES_TYPE = "resources";

  @Autowired
  private CommonAttributesConverter commonConverter;
  @Autowired
  private Converter<ResourceResult, List<Resource>> resourcesConverter;

  @Override
  public org.folio.rest.jaxrs.model.Title convert(TitleResult titleResult) {
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
          .withPublicationType(CommonAttributesConverter.publicationTypes.get(rmapiTitle.getPubType().toLowerCase()))
          .withSubjects(commonConverter.convertSubjects(rmapiTitle.getSubjectsList()))
          .withIdentifiers(commonConverter.convertIdentifiers(rmapiTitle.getIdentifiersList()))
          .withEdition(rmapiTitle.getEdition())
          .withContributors(commonConverter.convertContributors(rmapiTitle.getContributorsList()))
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
