package org.folio.rest.converter.util;

import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommonResourceConverter {

  @Autowired
  private CommonAttributesConverter commonConverter;

  public ResourceDataAttributes createResourceDataAttributes(Title title, CustomerResources resource) {
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
