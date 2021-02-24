package org.folio.rest.converter.titles;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.IterableUtils.matchesAny;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.titles.TitleConverterUtils.createEmptyResourcesRelationships;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.Subject;
import org.folio.repository.tag.DbTag;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Resources;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.TitleResult;

@Component
public class TitleConverter implements Converter<TitleResult, Title> {

  private static final String RESOURCES_TYPE = "resources";

  private static final PackageNameCustomComparator PACKAGE_NAME_CUSTOM_COMPARATOR = new PackageNameCustomComparator();

  @Autowired
  private Converter<ResourceResult, List<Resource>> resourcesConverter;
  @Autowired
  private Converter<List<Contributor>, List<Contributors>> contributorsConverter;
  @Autowired
  private Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
  @Autowired
  private Converter<List<Subject>, List<TitleSubject>> subjectsConverter;
  @Autowired
  private Converter<List<DbTag>, Tags> tagsConverter;

  @Override
  public Title convert(@NonNull TitleResult titleResult) {
    for (DbTag f:
         titleResult.getResourceTagList()) {

    }
    org.folio.holdingsiq.model.Title rmapiTitle = titleResult.getTitle();
    Title title = new Title()
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
          .withTags(titleResult.getTags())
        )
        .withRelationships(createEmptyResourcesRelationships())
      )
      .withIncluded(null)
      .withJsonapi(RestConstants.JSONAPI);

    title.getData().getAttributes()
      .setHasSelectedResources(matchesAny(rmapiTitle.getCustomerResourcesList(), CustomerResources::getIsSelected));

    includeResourcesIfNeeded(title, titleResult);
    return title;
  }

  private void includeResourcesIfNeeded(Title title, TitleResult titleResult) {
    org.folio.holdingsiq.model.Title rmapiTitle = titleResult.getTitle();
    List<CustomerResources> customerResourcesList = rmapiTitle.getCustomerResourcesList();
    if (titleResult.isIncludeResource() && nonNull(customerResourcesList)) {
      customerResourcesList.sort(PACKAGE_NAME_CUSTOM_COMPARATOR);
      List<ResourceCollectionItem> resourceCollectionItems = extractResourceCollectionItems(rmapiTitle);
      title.withIncluded(resourceCollectionItems).getData()
        .withRelationships(new Relationships()
          .withResources(new Resources()
            .withData(convertResourcesRelationship(customerResourcesList))
            .withMeta(new MetaDataIncluded().withIncluded(true))
          )
        );


      includeTagsIfNeeded(title, titleResult);
    }
  }

  private void includeTagsIfNeeded(Title title, TitleResult titleResult) {
    if (nonNull(titleResult.getResourceTagList())) {
      for (ResourceCollectionItem resourceCollectionItem : title.getIncluded()) {
        List<DbTag> tags = titleResult.getResourceTagList().stream()
          .filter(tag -> resourceCollectionItem.getId().equals(tag.getRecordId()))
          .collect(Collectors.toList());

        resourceCollectionItem.getAttributes().withTags(tagsConverter.convert(tags));
      }
    }
  }

  private List<ResourceCollectionItem> extractResourceCollectionItems(org.folio.holdingsiq.model.Title rmapiTitle) {
    ResourceResult resourceResult = new ResourceResult(rmapiTitle, null, null, false);
    return mapItems(resourcesConverter.convert(resourceResult), Resource::getData);
  }

  private List<RelationshipData> convertResourcesRelationship(List<CustomerResources> customerResources) {
    return mapItems(customerResources, resourceData -> new RelationshipData()
      .withId(IdParser.getResourceId(resourceData))
      .withType(RESOURCES_TYPE)
    );
  }

  private static class PackageNameCustomComparator implements Comparator<CustomerResources> {

    private static final Comparator<CustomerResources> NULL_SAFE_LENGTH_COMPARATOR = nullsLast(
      comparing(CustomerResources::getPackageName, comparingInt(String::length).reversed())
    );

    @Override
    public int compare(CustomerResources o1, CustomerResources o2) {
      if (StringUtils.isBlank(o1.getPackageName()) || StringUtils.isBlank(o2.getPackageName())) {
        return NULL_SAFE_LENGTH_COMPARATOR.compare(o1, o2);
      } else {
        return o1.getPackageName().compareToIgnoreCase(o2.getPackageName());
      }
    }
  }
}
