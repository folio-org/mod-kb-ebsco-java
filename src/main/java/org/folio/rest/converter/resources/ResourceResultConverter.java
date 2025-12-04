package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.IterableUtils.matchesAny;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.TitleResult;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S6813")
public class ResourceResultConverter implements Converter<ResourceResult, List<Resource>> {

  @Autowired
  private Converter<org.folio.holdingsiq.model.Title, ResourceCollectionItem> resourceCollectionItemConverter;
  @Autowired
  private CommonResourceConverter commonResourceConverter;
  @Autowired
  private Converter<TitleResult, org.folio.rest.jaxrs.model.Title> titlesConverter;
  @Autowired
  private Converter<VendorById, Provider> vendorConverter;
  @Autowired
  private Converter<PackageByIdData, Package> packageByIdConverter;

  @Override
  public List<Resource> convert(ResourceResult resourceResult) {
    var rmapiTitle = resourceResult.getTitle();
    PackageByIdData packageData = resourceResult.getPackageData();
    VendorById vendor = resourceResult.getVendor();
    AccessType accessType = resourceResult.getAccessType();
    boolean includeTitle = resourceResult.isIncludeTitle();

    List<CustomerResources> customerResourcesList = rmapiTitle.getCustomerResourcesList();
    boolean titleHasSelectedResources = matchesAny(customerResourcesList, CustomerResources::getIsSelected);
    return mapItems(customerResourcesList,
      convertToResource(rmapiTitle, titleHasSelectedResources, includeTitle, vendor, packageData, accessType));
  }

  private Function<CustomerResources, Resource> convertToResource(org.folio.holdingsiq.model.Title rmapiTitle,
                                                                  boolean titleHasSelectedResources,
                                                                  boolean includeTitle,
                                                                  VendorById vendor, PackageByIdData packageData,
                                                                  AccessType accessType) {
    return resource -> {
      Resource resultResource = new Resource()
        .withData(new ResourceCollectionItem()
          .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
          .withType(ResourceCollectionItem.Type.RESOURCES)
          .withAttributes(commonResourceConverter.createResourceDataAttributes(rmapiTitle, resource))
          .withRelationships(createEmptyRelationship())
        )
        .withIncluded(null)
        .withJsonapi(RestConstants.JSONAPI);

      resultResource.getData().getAttributes().setTitleHasSelectedResources(titleHasSelectedResources);
      includedResources(rmapiTitle, includeTitle, vendor, packageData, accessType, resultResource);
      return resultResource;
    };
  }

  private void includedResources(org.folio.holdingsiq.model.Title rmapiTitle, boolean includeTitle,
                                 @Nullable VendorById vendor, @Nullable PackageByIdData packageData,
                                 @Nullable AccessType accessType, Resource resultResource) {
    resultResource.setIncluded(new ArrayList<>());
    if (includeTitle) {
      includeTitle(rmapiTitle, resultResource);
    }
    if (vendor != null) {
      includeVendor(vendor, resultResource);
    }
    if (packageData != null) {
      includePackage(packageData, resultResource);
    }
    if (accessType != null) {
      includeAccessType(accessType, resultResource);
    }
  }

  private void includeAccessType(AccessType accessType, Resource resultResource) {
    resultResource.getIncluded().add(accessType);
    resultResource.getData()
      .getRelationships()
      .withAccessType(new HasOneRelationship()
        .withData(new RelationshipData()
          .withId(accessType.getId())
          .withType(AccessType.Type.ACCESS_TYPES.value()))
        .withMeta(new MetaDataIncluded()
          .withIncluded(true)));
  }

  private void includePackage(PackageByIdData packageData, Resource resultResource) {
    resultResource.getIncluded().add(requireNonNull(packageByIdConverter.convert(packageData)).getData());
    resultResource.getData()
      .getRelationships()
      .withPackage(new HasOneRelationship()
        .withData(new RelationshipData()
          .withId(packageData.getVendorId() + "-" + packageData.getPackageId())
          .withType(PACKAGES_TYPE)));
  }

  private void includeVendor(VendorById vendor, Resource resultResource) {
    resultResource.getIncluded().add(requireNonNull(vendorConverter.convert(vendor)).getData());
    resultResource.getData()
      .getRelationships()
      .withProvider(new HasOneRelationship()
        .withData(new RelationshipData()
          .withId(String.valueOf(vendor.getVendorId()))
          .withType(PROVIDERS_TYPE)));
  }

  private void includeTitle(org.folio.holdingsiq.model.Title rmapiTitle, Resource resultResource) {
    Title title = requireNonNull(titlesConverter.convert(new TitleResult(rmapiTitle, false)));
    resultResource.getIncluded().add(title.getData());
    resultResource.getData()
      .getRelationships()
      .withTitle(new HasOneRelationship()
        .withData(new RelationshipData()
          .withId(title.getData().getId())
          .withType(TITLES_TYPE)));
  }
}
