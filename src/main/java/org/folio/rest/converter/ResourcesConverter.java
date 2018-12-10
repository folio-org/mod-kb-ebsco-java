package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.*;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.*;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.folio.rest.util.RestConstants.*;

@Component
public class ResourcesConverter {

  @Autowired
  private CommonAttributesConverter commonConverter;
  @Autowired
  private TitleConverter titleConverter;
  @Autowired
  private VendorConverter vendorConverter;
  @Autowired
  private PackagesConverter packagesConverter;

  public List<Resource> convertFromRMAPIResource(Title title, VendorById vendor, PackageByIdData packageData, boolean includeTitle) {
    return title.getCustomerResourcesList().stream().map(resource -> {
      Resource resultResource = new org.folio.rest.jaxrs.model.Resource()
        .withData(new ResourceCollectionItem()
          .withId(String.valueOf(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId()))
          .withType(ResourceCollectionItem.Type.RESOURCES)
          .withAttributes(createResourceDataAttributes(title, resource))
          .withRelationships(createEmptyRelationship())
        )
        .withIncluded(null)
        .withJsonapi(RestConstants.JSONAPI);
      resultResource.setIncluded(new ArrayList<>());
      if (includeTitle) {
        resultResource.getIncluded().add(titleConverter.convertFromRMAPITitle(title, null).getData());
        resultResource.getData()
          .getRelationships()
          .withTitle(new HasOneRelationship()
            .withData(new RelationshipData()
              .withId(String.valueOf(title.getTitleId()))
              .withType(TITLES_TYPE)));
      }
      if(vendor != null){
        resultResource.getIncluded().add(vendorConverter.convertToProvider(vendor).getData());
        resultResource.getData()
          .getRelationships()
          .withProvider(new HasOneRelationship()
            .withData(new RelationshipData()
              .withId(String.valueOf(vendor.getVendorId()))
              .withType(PROVIDERS_TYPE)));
      }
      if(packageData != null){
        resultResource.getIncluded().add(packagesConverter.convert(packageData).getData());
        resultResource.getData()
          .getRelationships()
          .withPackage(new HasOneRelationship()
            .withData(new RelationshipData()
              .withId(String.valueOf(packageData.getVendorId() + "-" + packageData.getPackageId()))
              .withType(PACKAGES_TYPE)));
      }
      return resultResource;
    }).collect(toList());
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

  public ResourceCollection convertFromRMAPIResourceList(Titles titles) {

    List<ResourceCollectionItem> titleList = titles.getTitleList().stream()
        .map(this::convertResource)
        .collect(Collectors.toList());
      return new ResourceCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
        .withData(titleList);

  }

  private ResourceCollectionItem convertResource(Title title) {
    CustomerResources resource = title.getCustomerResourcesList().get(0);
    return new ResourceCollectionItem()
    .withId(String.valueOf(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId()))
    .withType(ResourceCollectionItem.Type.RESOURCES)
      .withRelationships(createEmptyRelationship())
    .withAttributes(createResourceDataAttributes(title, resource));
  }


  private ResourceDataAttributes createResourceDataAttributes(Title title, CustomerResources resource) {
     return new ResourceDataAttributes()
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
      .withProxy(commonConverter.convertProxy(resource.getProxy()));
  }
}
