package org.folio.rest.converter;

import java.util.ArrayList;

import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourceRelationships;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;

public class ResourcesConverter {
  private CommonAttributesConverter commonConverter;

  private TitleConverter titleConverter;

  public ResourcesConverter() {
    this(new CommonAttributesConverter(), new TitleConverter());
  }

  public ResourcesConverter(CommonAttributesConverter commonConverter, TitleConverter titleConverter) {
    this.commonConverter = commonConverter;
    this.titleConverter = titleConverter;
  }

  public Resource convertFromRMAPIResource(Title title) {
    return convertFromRMAPIResource(title, false);
  }

  public Resource convertFromRMAPIResource(Title title, boolean includeTitle) {
    CustomerResources resource = title.getCustomerResourcesList().get(0);

    Resource resultResource = new Resource()
      .withData(new ResourceCollectionItem()
        .withId(String.valueOf(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId()))
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withAttributes(new ResourceDataAttributes()
          .withDescription(title.getDescription())
          .withEdition(title.getEdition())
          .withIsPeerReviewed(title.getIsPeerReviewed())
          .withIsTitleCustom(title.getIsTitleCustom())
          .withPublisherName(title.getPublisherName())
          .withTitleId(title.getTitleId())
          .withContributors(commonConverter.convertContributors(title.getContributorsList()))
          .withIdentifiers(commonConverter.convertIdentifiers(title.getIdentifiersList()))
          .withName(title.getTitleName())
          .withPublicationType(CommonAttributesConverter.publicationTypes.get(title.getPubType().toLowerCase()))
          .withSubjects(commonConverter.convertSubjects(title.getSubjectsList()))
          .withCoverageStatement(resource.getCoverageStatement())
          .withCustomEmbargoPeriod(commonConverter.convertEmbargo(resource.getCustomEmbargoPeriod()))
          .withIsPackageCustom(resource.getIsPackageCustom())
          .withIsSelected(resource.getIsSelected())
          .withIsTokenNeeded(resource.getIsTokenNeeded())
          .withLocationId(resource.getLocationId())
          .withManagedEmbargoPeriod(commonConverter.convertEmbargo(resource.getManagedEmbargoPeriod()))
          .withPackageId(String.valueOf(resource.getVendorId() + "-" + resource.getPackageId()))
          .withPackageName(resource.getPackageName())
          .withUrl(resource.getUrl())
          .withProviderId(resource.getVendorId())
          .withProviderName(resource.getVendorName())
          .withVisibilityData(commonConverter.convertVisibilityData(resource.getVisibilityData()))
          .withManagedCoverages(commonConverter.convertCoverages(resource.getManagedCoverageList()))
          .withCustomCoverages(commonConverter.convertCoverages(resource.getCustomCoverageList()))
          .withProxy(commonConverter.convertProxy(resource.getProxy())))
        .withRelationships(createEmptyRelationship())
      )
      .withIncluded(null)
      .withJsonapi(RestConstants.JSONAPI);

    resultResource.setIncluded(new ArrayList<>());
    if (includeTitle) {
      resultResource.getIncluded().add(titleConverter.convertFromRMAPITitle(title).getData());
      resultResource.getData()
        .getRelationships()
          .withTitle(new HasOneRelationship()
            .withData(new RelationshipData()
              .withId(String.valueOf(title.getTitleId()))
              .withType("titles")));
    }
    return resultResource;
  }

  private ResourceRelationships createEmptyRelationship() {
    return new ResourceRelationships()
      .withProvider(new HasOneRelationship()
        .withMeta(
          new MetaDataIncluded()
            .withIncluded(false)))
      .withPackage(new HasOneRelationship().withMeta(
        new MetaDataIncluded()
          .withIncluded(false)))
      .withTitle(new HasOneRelationship().withMeta(
        new MetaDataIncluded()
          .withIncluded(false)));
  }
}
