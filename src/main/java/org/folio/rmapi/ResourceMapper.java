package org.folio.rmapi;

import java.util.ArrayList;
import java.util.List;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.Title;

/**
 * Maps between the flat v2 {@link Resource} (a title within one package) and the nested {@link Title} with its
 * {@link CustomerResources}, for the places where nested titles and resources meet.
 */
public final class ResourceMapper {

  private ResourceMapper() {
  }

  /**
   * Builds the flat resource for one of the title's customer resources. Title relationships are not copied:
   * they belong to the title, not to each of its resources.
   */
  @SuppressWarnings("checkstyle:MethodLength")
  public static Resource toResource(Title title, CustomerResources customerResource) {
    return Resource.builder()
      .titleId(title.getTitleId())
      .titleName(title.getTitleName())
      .publisherName(title.getPublisherName())
      .identifiersList(title.getIdentifiersList())
      .subjectsList(title.getSubjectsList())
      .isTitleCustom(title.getIsTitleCustom())
      .alternateTitleList(title.getAlternateTitleList())
      .pubType(title.getPubType())
      .description(title.getDescription())
      .edition(title.getEdition())
      .isPeerReviewed(title.getIsPeerReviewed())
      .contributorsList(title.getContributorsList())
      .packageId(customerResource.getPackageId())
      .packageName(customerResource.getPackageName())
      .packageType(customerResource.getPackageType())
      .proxy(customerResource.getProxy())
      .isPrimaryPackage(customerResource.getIsPrimaryPackage())
      .isPackageCustom(customerResource.getIsPackageCustom())
      .vendorId(customerResource.getVendorId())
      .vendorName(customerResource.getVendorName())
      .locationId(customerResource.getLocationId())
      .isSelected(customerResource.getIsSelected())
      .isOrderedThroughEbsco(customerResource.getIsOrderedThroughEbsco())
      .isTokenNeeded(customerResource.getIsTokenNeeded())
      .kbartExcluded(customerResource.getKbartExcluded())
      .kbartCoverageOwnership(customerResource.getKbartCoverageOwnership())
      .visibilityDetails(customerResource.getVisibilityDetails())
      .managedCoverageList(customerResource.getManagedCoverageList())
      .customCoverageList(customerResource.getCustomCoverageList())
      .coverageStatement(customerResource.getCoverageStatement())
      .managedEmbargoPeriod(customerResource.getManagedEmbargoPeriod())
      .customEmbargoPeriod(customerResource.getCustomEmbargoPeriod())
      .url(customerResource.getUrl())
      .consortia(customerResource.getConsortia())
      .userDefinedFields(customerResource.getUserDefinedFields())
      .build();
  }

  /**
   * Builds the package-specific part of the resource.
   */
  @SuppressWarnings("checkstyle:MethodLength")
  public static CustomerResources toCustomerResources(Resource resource) {
    return CustomerResources.builder()
      .titleId(resource.getTitleId())
      .packageId(resource.getPackageId())
      .packageName(resource.getPackageName())
      .packageType(resource.getPackageType())
      .proxy(resource.getProxy())
      .isPackageCustom(resource.getIsPackageCustom())
      .vendorId(resource.getVendorId())
      .vendorName(resource.getVendorName())
      .locationId(resource.getLocationId())
      .isSelected(resource.getIsSelected())
      .isOrderedThroughEbsco(resource.getIsOrderedThroughEbsco())
      .isTokenNeeded(resource.getIsTokenNeeded())
      .visibilityDetails(resource.getVisibilityDetails())
      .managedCoverageList(resource.getManagedCoverageList())
      .customCoverageList(resource.getCustomCoverageList())
      .coverageStatement(resource.getCoverageStatement())
      .managedEmbargoPeriod(resource.getManagedEmbargoPeriod())
      .customEmbargoPeriod(resource.getCustomEmbargoPeriod())
      .url(resource.getUrl())
      .isPrimaryPackage(resource.getIsPrimaryPackage())
      .kbartExcluded(resource.getKbartExcluded())
      .kbartCoverageOwnership(resource.getKbartCoverageOwnership())
      .consortia(resource.getConsortia())
      .userDefinedFields(resource.getUserDefinedFields())
      .build();
  }

  /**
   * Builds a title that contains only this resource's package in its customer resources.
   */
  public static Title toTitle(Resource resource) {
    return Title.builder()
      .titleId(resource.getTitleId())
      .titleName(resource.getTitleName())
      .publisherName(resource.getPublisherName())
      .identifiersList(resource.getIdentifiersList())
      .subjectsList(resource.getSubjectsList())
      .isTitleCustom(resource.getIsTitleCustom())
      .alternateTitleList(resource.getAlternateTitleList())
      .pubType(resource.getPubType())
      .customerResourcesList(new ArrayList<>(List.of(toCustomerResources(resource))))
      .description(resource.getDescription())
      .edition(resource.getEdition())
      .isPeerReviewed(resource.getIsPeerReviewed())
      .contributorsList(resource.getContributorsList())
      .titleRelationships(resource.getTitleRelationships())
      .build();
  }
}
