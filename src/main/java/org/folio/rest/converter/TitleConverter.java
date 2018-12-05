package org.folio.rest.converter;

import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.Resources;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleListDataAttributes;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.TitlePost;
import org.folio.rmapi.model.Titles;

public class TitleConverter {

  private static final String INCLUDE_RESOURCES_VALUE = "resources";
  private static final String RESOURCES_TYPE = "resources";

  private CommonAttributesConverter commonConverter;

  private ResourcesConverter resourcesConverter;

  private static final Relationships EMPTY_RESOURCES_RELATIONSHIP = new Relationships()
    .withResources(new Resources()
      .withMeta(new MetaDataIncluded()
        .withIncluded(false))
      .withData(null));

  public TitleConverter() {
    this(new CommonAttributesConverter(), new ResourcesConverter());
  }

  public TitleConverter(CommonAttributesConverter commonConverter, ResourcesConverter resourcesConverter) {
    this.commonConverter = commonConverter;
    this.resourcesConverter = resourcesConverter;
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

  public org.folio.rest.jaxrs.model.Title convertFromRMAPITitle(org.folio.rmapi.model.Title rmapiTitle, String include) {
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
        .withRelationships(EMPTY_RESOURCES_RELATIONSHIP)
      )
      .withIncluded(null)
      .withJsonapi(RestConstants.JSONAPI);
    if (INCLUDE_RESOURCES_VALUE.equalsIgnoreCase(include) && Objects.nonNull(customerResourcesList)) {
      title
        .withIncluded(resourcesConverter.convertFromRMAPIResource(rmapiTitle, null, null, false)
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

  private org.folio.rest.jaxrs.model.Titles convertTitle(Title title) {
    return new org.folio.rest.jaxrs.model.Titles()
      .withId(String.valueOf(title.getTitleId()))
      .withRelationships(EMPTY_RESOURCES_RELATIONSHIP)
      .withType(TITLES_TYPE)
      .withAttributes(new TitleListDataAttributes()
        .withName(title.getTitleName())
        .withPublisherName(title.getPublisherName())
        .withIsTitleCustom(title.getIsTitleCustom())
        .withPublicationType(CommonAttributesConverter.publicationTypes.get(title.getPubType().toLowerCase()))
        .withSubjects(commonConverter.convertSubjects(title.getSubjectsList()))
        .withIdentifiers(commonConverter.convertIdentifiers(title.getIdentifiersList())));
  }

  public TitlePost convertToPost(TitlePostRequest entity) {
    Boolean isPeerReviewed = entity.getData().getAttributes().getIsPeerReviewed();
    TitlePost.TitlePostBuilder titlePost = TitlePost.builder()
      .titleName(entity.getData().getAttributes().getName())
      .description(entity.getData().getAttributes().getDescription())
      .edition(entity.getData().getAttributes().getEdition())
      .isPeerReviewed(java.util.Objects.isNull(isPeerReviewed) ? Boolean.FALSE : isPeerReviewed)
      .publisherName(entity.getData().getAttributes().getPublisherName())
      .pubType(CommonAttributesConverter.publicationTypes.inverseBidiMap().get(entity.getData().getAttributes().getPublicationType()));

    List<org.folio.rest.jaxrs.model.Identifier> identifiersList = entity.getData().getAttributes().getIdentifiers();
    if(!identifiersList.isEmpty()) {
      titlePost.identifiersList(commonConverter.convertToIdentifiers(identifiersList));
    }

    List<org.folio.rest.jaxrs.model.Contributors> contributorsList = entity.getData().getAttributes().getContributors();
    if(!contributorsList.isEmpty()){
      titlePost.contributorsList(commonConverter.convertToContributors(contributorsList));
    }

    return titlePost.build();
  }
}
