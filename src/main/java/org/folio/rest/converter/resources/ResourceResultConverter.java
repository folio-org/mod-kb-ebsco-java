package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.folio.holdingsiq.model.PackageData;
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
import org.folio.rest.util.IdParser;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.ResourceMapper;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.TitleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S6813")
public class ResourceResultConverter implements Converter<ResourceResult, List<Resource>> {

  @Autowired
  private CommonResourceConverter commonResourceConverter;
  @Autowired
  private Converter<TitleResult, org.folio.rest.jaxrs.model.Title> titlesConverter;
  @Autowired
  private Converter<VendorById, Provider> vendorConverter;
  @Autowired
  private Converter<PackageData, Package> packageByIdConverter;

  @Override
  public List<Resource> convert(ResourceResult resourceResult) {
    var rmapiResource = resourceResult.getResource();
    Resource resultResource = new Resource()
      .withData(new ResourceCollectionItem()
        .withId(IdParser.getResourceId(rmapiResource))
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withAttributes(commonResourceConverter.createResourceDataAttributes(rmapiResource))
        .withRelationships(createEmptyRelationship())
      )
      .withIncluded(null)
      .withJsonapi(RestConstants.JSONAPI);

    // the resource is the only package of its title in the response, so it decides whether a resource is selected
    resultResource.getData().getAttributes()
      .setTitleHasSelectedResources(BooleanUtils.isTrue(rmapiResource.getIsSelected()));
    includedResources(resourceResult, resultResource);
    return List.of(resultResource);
  }

  private void includedResources(ResourceResult resourceResult, Resource resultResource) {
    resultResource.setIncluded(new ArrayList<>());
    if (resourceResult.isIncludeTitle()) {
      includeTitle(resourceResult.getResource(), resultResource);
    }
    if (resourceResult.getVendor() != null) {
      includeVendor(resourceResult.getVendor(), resultResource);
    }
    if (resourceResult.getPackageData() != null) {
      includePackage(resourceResult.getPackageData(), resultResource);
    }
    if (resourceResult.getAccessType() != null) {
      includeAccessType(resourceResult.getAccessType(), resultResource);
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

  private void includePackage(PackageData packageData, Resource resultResource) {
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

  private void includeTitle(org.folio.holdingsiq.model.Resource rmapiResource, Resource resultResource) {
    // relationships are part of the resource attributes, so they are not repeated in the included title
    var rmapiTitle = ResourceMapper.toTitle(rmapiResource).toBuilder().titleRelationships(null).build();
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
