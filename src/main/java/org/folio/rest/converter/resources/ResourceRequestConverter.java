package org.folio.rest.converter.resources;

import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rmapi.model.Contributor;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.EmbargoPeriod;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.ResourcePut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceRequestConverter {

  @Autowired
  private Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter;
  @Autowired
  private Converter<List<Contributors>, List<Contributor>> toContributorsConverter;


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
      builder.identifiersList(toIdentifiersConverter.convert(attributes.getIdentifiers()));
    }
    if (attributes.getContributors() != null && !attributes.getContributors().isEmpty()) {
      builder.contributorsList(toContributorsConverter.convert(attributes.getContributors()));
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
