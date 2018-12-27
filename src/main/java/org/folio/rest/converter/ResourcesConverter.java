package org.folio.rest.converter;

import static java.util.stream.Collectors.toList;

import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.model.ResourceRelationships;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.EmbargoPeriod;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.ResourcePut;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rmapi.model.VendorById;

@Component
public class ResourcesConverter {

  @Autowired
  private CommonAttributesConverter commonConverter;
  @Autowired
  private TitleConverter titleConverter;
  @Autowired
  private Converter<VendorById, Provider> vendorConverter;
  @Autowired
  private Converter<PackageByIdData, Package> packageByIdConverter;

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
        resultResource.getIncluded().add(vendorConverter.convert(vendor).getData());
        resultResource.getData()
          .getRelationships()
          .withProvider(new HasOneRelationship()
            .withData(new RelationshipData()
              .withId(String.valueOf(vendor.getVendorId()))
              .withType(PROVIDERS_TYPE)));
      }
      if(packageData != null){
        resultResource.getIncluded().add(packageByIdConverter.convert(packageData).getData());
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

  public ResourceCollectionItem convertResource(Title title) {
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

  public org.folio.rmapi.model.ResourcePut convertToRMAPIResourcePutRequest(ResourcePutRequest entity) {
    ResourceDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes);
    return builder.build();
  }

  public ResourcePut convertToRMAPICustomResourcePutRequest(ResourcePutRequest entity) {
    ResourceDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes);
    //Map attributes specific to custom resources to RM API fields
    builder.titleName(attributes.getName());
    builder.publisherName(attributes.getPublisherName());
    builder.edition(attributes.getEdition());
    builder.description(attributes.getDescription());
    builder.url(attributes.getUrl());
    if (attributes.getIdentifiers() != null && !attributes.getIdentifiers().isEmpty()) {
      builder.identifiersList(commonConverter.convertToIdentifiers(attributes.getIdentifiers()));
    }
    if (attributes.getContributors() != null && !attributes.getContributors().isEmpty()) {
      builder.contributorsList(commonConverter.convertToContributors(attributes.getContributors()));
    }
    return builder.build();
  }

  private ResourcePut.ResourcePutBuilder convertCommonAttributesToResourcePutRequest(ResourceDataAttributes attributes) {
    ResourcePut.ResourcePutBuilder builder = ResourcePut.builder();

    builder.isSelected(attributes.getIsSelected());

    if (attributes.getProxy() != null) {
      //RM API gives an error when we pass inherited as true along with updated proxy value
      //Hard code it to false; it should not affect the state of inherited that RM API maintains
      org.folio.rmapi.model.Proxy proxy = org.folio.rmapi.model.Proxy.builder()
        .id(attributes.getProxy().getId())
        .inherited(false)
        .build();
      builder.proxy(proxy);
    }

    if (attributes.getVisibilityData() != null) {
      builder.isHidden(attributes.getVisibilityData().getIsHidden());
    }

    if (attributes.getCoverageStatement() != null) {
      builder.coverageStatement(attributes.getCoverageStatement());
    }

    if (attributes.getCustomEmbargoPeriod() != null) {
      EmbargoUnit embargoUnit = attributes.getCustomEmbargoPeriod().getEmbargoUnit();
      EmbargoPeriod customEmbargo = EmbargoPeriod.builder()
          .embargoUnit(embargoUnit != null ? embargoUnit.value() : null)
          .embargoValue(attributes.getCustomEmbargoPeriod().getEmbargoValue())
          .build();
      builder.customEmbargoPeriod(customEmbargo);
    }

    if (attributes.getCustomCoverages() != null && !attributes.getCustomCoverages().isEmpty()) {
      builder.customCoverageList(convertToRMAPICustomCoverageList(attributes.getCustomCoverages()));
    }

    // For now, we do not have any attributes specific to managed resources to be mapped to RM API fields
    // but below, we set the same values as we conduct a GET for pubType and isPeerReviewed because otherwise RM API gives
    // a bad request error if those values are set to null. All of the other fields are retained as is by RM API because they
    // cannot be updated.
    builder.pubType(attributes.getPublicationType() != null ? attributes.getPublicationType().value() : null);
    builder.isPeerReviewed(attributes.getIsPeerReviewed());

    return builder;
  }

  private List<CoverageDates> convertToRMAPICustomCoverageList(List<Coverage> customCoverages) {
    return customCoverages.stream().map(coverage -> CoverageDates.builder()
        .beginCoverage(coverage.getBeginCoverage())
        .endCoverage(coverage.getEndCoverage())
        .build())
        .collect(Collectors.toList());
  }
}
