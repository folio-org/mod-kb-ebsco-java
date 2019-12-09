package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.Title;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;

@Component
public class ResourceRequestConverter {
  public org.folio.holdingsiq.model.ResourcePut convertToRMAPIResourcePutRequest(ResourcePutRequest entity, Title oldTitle) {
    ResourcePutDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldTitle);
    return builder.build();
  }

  public ResourcePut convertToRMAPICustomResourcePutRequest(ResourcePutRequest entity, Title oldTitle) {
    ResourcePutDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    CustomerResources oldResource = oldTitle.getCustomerResourcesList().get(0);
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldTitle);
    //Map attributes specific to custom resources to RM API fields
    builder.titleName(oldTitle.getTitleName());
    builder.publisherName(oldTitle.getPublisherName());
    builder.edition(oldTitle.getEdition());
    builder.description(oldTitle.getDescription());
    builder.url(valueOrDefault(attributes.getUrl(), oldResource.getUrl()));

    builder.identifiersList(oldTitle.getIdentifiersList());
    builder.contributorsList(oldTitle.getContributorsList());
    return builder.build();
  }

  private ResourcePut.ResourcePutBuilder convertCommonAttributesToResourcePutRequest(ResourcePutDataAttributes attributes, Title oldTitle) {
    ResourcePut.ResourcePutBuilder builder = ResourcePut.resourcePutBuilder();
    CustomerResources oldResource = oldTitle.getCustomerResourcesList().get(0);
    builder.isSelected(valueOrDefault(attributes.getIsSelected(), oldResource.getIsSelected()));

    Proxy proxy = attributes.getProxy();
    String proxyId = proxy != null && proxy.getId() != null ? proxy.getId() : oldResource.getProxy().getId();
    //RM API gives an error when we pass inherited as true along with updated proxy value
    //Hard code it to false; it should not affect the state of inherited that RM API maintains
    org.folio.holdingsiq.model.Proxy rmApiProxy = org.folio.holdingsiq.model.Proxy.builder()
      .id(proxyId)
      .inherited(false)
      .build();
    builder.proxy(rmApiProxy);

    Boolean oldHidden = oldResource.getVisibilityData() != null ? oldResource.getVisibilityData().getIsHidden() : null;
    Boolean isHidden = attributes.getVisibilityData() != null && attributes.getVisibilityData().getIsHidden() != null ?
      attributes.getVisibilityData().getIsHidden() : oldHidden;

    builder.isHidden(isHidden);

    String coverageStatement = valueOrDefault(attributes.getCoverageStatement(), oldResource.getCoverageStatement());
    builder.coverageStatement(coverageStatement);

    org.folio.rest.jaxrs.model.EmbargoPeriod embargoPeriod = attributes.getCustomEmbargoPeriod();

    String oldEmbargoUnit = oldResource.getCustomEmbargoPeriod() != null ? oldResource.getCustomEmbargoPeriod().getEmbargoUnit() : null;
    String embargoUnit = embargoPeriod != null && embargoPeriod.getEmbargoUnit() != null ?
      embargoPeriod.getEmbargoUnit().value() : oldEmbargoUnit;
    int oldEmbargoValue = oldResource.getCustomEmbargoPeriod() != null ?
      oldResource.getCustomEmbargoPeriod().getEmbargoValue() : 0;
    int embargoValue = embargoPeriod != null && embargoPeriod.getEmbargoValue() != null ?
      embargoPeriod.getEmbargoValue() : oldEmbargoValue;
    EmbargoPeriod customEmbargo = EmbargoPeriod.builder()
      .embargoUnit(embargoUnit)
      .embargoValue(embargoValue)
      .build();
    builder.customEmbargoPeriod(customEmbargo);

    List<CoverageDates> coverageDates = attributes.getCustomCoverages() != null ?
      convertToRMAPICustomCoverageList(attributes.getCustomCoverages()) : oldResource.getCustomCoverageList();
    builder.customCoverageList(coverageDates);

    // For now, we do not have any attributes specific to managed resources to be mapped to RM API fields
    // but below, we set the same values as we conduct a GET for pubType and isPeerReviewed because otherwise RM API gives
    // a bad request error if those values are set to null. All of the other fields are retained as is by RM API because they
    // cannot be updated.
    builder.pubType(oldTitle.getPubType());
    builder.isPeerReviewed(oldTitle.getIsPeerReviewed());

    builder.userDefinedField1(valueOrDefault(attributes.getUserDefinedField1(), oldResource.getUserDefinedField1()));
    builder.userDefinedField2(valueOrDefault(attributes.getUserDefinedField2(), oldResource.getUserDefinedField2()));
    builder.userDefinedField3(valueOrDefault(attributes.getUserDefinedField3(), oldResource.getUserDefinedField3()));
    builder.userDefinedField4(valueOrDefault(attributes.getUserDefinedField4(), oldResource.getUserDefinedField4()));
    builder.userDefinedField5(valueOrDefault(attributes.getUserDefinedField5(), oldResource.getUserDefinedField5()));
    return builder;
  }

  private <T> T valueOrDefault(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  private List<CoverageDates> convertToRMAPICustomCoverageList(List<Coverage> customCoverages) {
    return mapItems(customCoverages,
      coverage -> CoverageDates.builder()
        .beginCoverage(coverage.getBeginCoverage())
        .endCoverage(coverage.getEndCoverage())
        .build());
  }
}
