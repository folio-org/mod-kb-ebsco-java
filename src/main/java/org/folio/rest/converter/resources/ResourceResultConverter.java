package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.TitleResult;

@Component
public class ResourceResultConverter implements Converter<ResourceResult, List<Resource>> {

  @Autowired
  private Converter<Title, ResourceCollectionItem> resourceCollectionItemConverter;
  @Autowired
  private CommonResourceConverter commonResourceConverter;
  @Autowired
  private Converter<TitleResult, org.folio.rest.jaxrs.model.Title> titlesConverter;
  @Autowired
  private Converter<VendorById, Provider> vendorConverter;
  @Autowired
  private Converter<PackageByIdData, Package> packageByIdConverter;

  @Override
  public List<Resource> convert(@NonNull ResourceResult resourceResult) {
    Title title = resourceResult.getTitle();
    PackageByIdData packageData = resourceResult.getPackageData();
    VendorById vendor = resourceResult.getVendor();
    boolean includeTitle = resourceResult.isIncludeTitle();

    return mapItems(title.getCustomerResourcesList(),
      resource -> {
        Resource resultResource = new org.folio.rest.jaxrs.model.Resource()
          .withData(new ResourceCollectionItem()
            .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
            .withType(ResourceCollectionItem.Type.RESOURCES)
            .withAttributes(commonResourceConverter.createResourceDataAttributes(title, resource))
            .withRelationships(createEmptyRelationship())
          )
          .withIncluded(null)
          .withJsonapi(RestConstants.JSONAPI);
        resultResource.setIncluded(new ArrayList<>());
        if (includeTitle) {
          resultResource.getIncluded().add(titlesConverter.convert(new TitleResult(title, false)).getData());
          resultResource.getData()
            .getRelationships()
            .withTitle(new HasOneRelationship()
              .withData(new RelationshipData()
                .withId(String.valueOf(title.getTitleId()))
                .withType(TITLES_TYPE)));
        }
        if (vendor != null) {
          resultResource.getIncluded().add(vendorConverter.convert(vendor).getData());
          resultResource.getData()
            .getRelationships()
            .withProvider(new HasOneRelationship()
              .withData(new RelationshipData()
                .withId(String.valueOf(vendor.getVendorId()))
                .withType(PROVIDERS_TYPE)));
        }
        if (packageData != null) {
          resultResource.getIncluded().add(packageByIdConverter.convert(packageData).getData());
          resultResource.getData()
            .getRelationships()
            .withPackage(new HasOneRelationship()
              .withData(new RelationshipData()
                .withId(packageData.getVendorId() + "-" + packageData.getPackageId())
                .withType(PACKAGES_TYPE)));
        }
        return resultResource;
      });

  }
}
