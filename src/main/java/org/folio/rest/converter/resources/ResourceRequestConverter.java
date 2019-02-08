package org.folio.rest.converter.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rmapi.model.Contributor;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.EmbargoPeriod;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.ResourcePut;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.result.ResourceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceRequestConverter {

  @Autowired
  private Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter;
  @Autowired
  private Converter<List<Contributors>, List<Contributor>> toContributorsConverter;


  public org.folio.rmapi.model.ResourcePut convertToRMAPIResourcePutRequest(ResourcePutRequest entity, ResourceResult oldResourceResponse) {
    ResourceDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldResourceResponse.getTitle().getCustomerResourcesList().get(0));
    return builder.build();
  }

  public ResourcePut convertToRMAPICustomResourcePutRequest(ResourcePutRequest entity, ResourceResult oldResourceResponse) {
    ResourceDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    Title oldTitle = oldResourceResponse.getTitle();
    CustomerResources oldResource = oldTitle.getCustomerResourcesList().get(0);
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldResource);
    //Map attributes specific to custom resources to RM API fields
    builder.titleName(attributes.getName());
    builder.publisherName(attributes.getPublisherName());
    builder.edition(attributes.getEdition());
    builder.description(attributes.getDescription());
    String url = attributes.getUrl() != null ? attributes.getUrl() : oldResource.getUrl();
    builder.url(url);

    List<Identifier> identifierList = attributes.getIdentifiers() != null && !attributes.getIdentifiers().isEmpty() ?
      toIdentifiersConverter.convert(attributes.getIdentifiers()) : oldTitle.getIdentifiersList();
    builder.identifiersList(identifierList);
    List<Contributor> contributorList = attributes.getContributors() != null && !attributes.getContributors().isEmpty() ?
      toContributorsConverter.convert(attributes.getContributors()) : oldTitle.getContributorsList();
    builder.contributorsList(contributorList);
    return builder.build();
  }

  private ResourcePut.ResourcePutBuilder convertCommonAttributesToResourcePutRequest(ResourceDataAttributes attributes, CustomerResources resource) {
    ResourcePut.ResourcePutBuilder builder = ResourcePut.builder();

    builder.isSelected((attributes.getIsSelected() != null ? attributes.getIsSelected() : resource.getIsSelected()));

    Proxy proxy = attributes.getProxy();
    String proxyId = proxy != null && proxy.getId() != null ? proxy.getId() : resource.getProxy().getId();
    //RM API gives an error when we pass inherited as true along with updated proxy value
    //Hard code it to false; it should not affect the state of inherited that RM API maintains
    org.folio.rmapi.model.Proxy rmApiProxy = org.folio.rmapi.model.Proxy.builder()
      .id(proxyId)
      .inherited(false)
      .build();
    builder.proxy(rmApiProxy);

    Boolean oldHidden = resource.getVisibilityData() != null ? resource.getVisibilityData().getIsHidden() : null;
    Boolean isHidden = attributes.getVisibilityData() != null && attributes.getVisibilityData().getIsHidden() != null ?
      attributes.getVisibilityData().getIsHidden() : oldHidden;

    builder.isHidden(isHidden);

    String coverageStatement = attributes.getCoverageStatement() != null ?
      attributes.getCoverageStatement() : resource.getCoverageStatement();
    builder.coverageStatement(coverageStatement);

    org.folio.rest.jaxrs.model.EmbargoPeriod embargoPeriod = attributes.getCustomEmbargoPeriod();

    String oldEmbargoUnit = resource.getCustomEmbargoPeriod() != null ? resource.getCustomEmbargoPeriod().getEmbargoUnit() : null;
    String embargoUnit = embargoPeriod != null && embargoPeriod.getEmbargoUnit() != null ?
      embargoPeriod.getEmbargoUnit().value() : oldEmbargoUnit;
    int oldEmbargoValue = resource.getCustomEmbargoPeriod() != null ?
      resource.getCustomEmbargoPeriod().getEmbargoValue() : 0;
    int embargoValue = embargoPeriod != null && embargoPeriod.getEmbargoValue() != null ?
      embargoPeriod.getEmbargoValue() : oldEmbargoValue;
    EmbargoPeriod customEmbargo = EmbargoPeriod.builder()
      .embargoUnit(embargoUnit)
      .embargoValue(embargoValue)
      .build();
    builder.customEmbargoPeriod(customEmbargo);

    List<CoverageDates> coverageDates = attributes.getCustomCoverages() != null && !attributes.getCustomCoverages().isEmpty() ?
      convertToRMAPICustomCoverageList(attributes.getCustomCoverages()) : resource.getCustomCoverageList();
    builder.customCoverageList(coverageDates);

    // For now, we do not have any attributes specific to managed resources to be mapped to RM API fields
    // but below, we set the same values as we conduct a GET for pubType and isPeerReviewed because otherwise RM API gives
    // a bad request error if those values are set to null. All of the other fields are retained as is by RM API because they
    // cannot be updated.
    builder.pubType(attributes.getPublicationType() != null ? ConverterConsts.publicationTypes.inverseBidiMap().get(attributes.getPublicationType()) : null);
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
