package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourceRelationships;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;

public class ResourcesConverter {
  
  private static final ResourceRelationships EMPTY_RELATIONSHIPS = new ResourceRelationships()
      .withProvider(new MetaIncluded().withMeta(
        new MetaDataIncluded()
          .withIncluded(false)))
      .withPackage(new MetaIncluded().withMeta(
          new MetaDataIncluded()
            .withIncluded(false)))
      .withTitle(new MetaIncluded().withMeta(
          new MetaDataIncluded()
            .withIncluded(false)));    

  private CommonAttributesConverter commonConverter;
  
  public ResourcesConverter() {
    this(new CommonAttributesConverter());
  }
  
  public ResourcesConverter(CommonAttributesConverter commonConverter) {
    this.commonConverter = commonConverter;
  }

  public Resource convertFromRMAPIResource(Title title) {
    CustomerResources resource = title.getCustomerResourcesList().get(0);
    return new org.folio.rest.jaxrs.model.Resource()
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
          .withRelationships(EMPTY_RELATIONSHIPS)
          )
        .withIncluded(null)
        .withJsonapi(RestConstants.JSONAPI);
  }
}
