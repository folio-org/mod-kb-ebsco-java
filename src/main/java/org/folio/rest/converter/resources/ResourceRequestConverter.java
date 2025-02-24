package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.springframework.stereotype.Component;

@Component
public class ResourceRequestConverter {
  public org.folio.holdingsiq.model.ResourcePut convertToRmApiResourcePutRequest(ResourcePutRequest entity,
                                                                                 Title oldTitle) {
    ResourcePutDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldTitle);
    return builder.build();
  }

  public ResourcePut convertToRmApiCustomResourcePutRequest(ResourcePutRequest entity, Title oldTitle) {
    ResourcePutDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldTitle);
    //Map attributes specific to custom resources to RM API fields
    builder.titleName(oldTitle.getTitleName());
    builder.publisherName(oldTitle.getPublisherName());
    builder.edition(oldTitle.getEdition());
    builder.description(oldTitle.getDescription());

    builder.identifiersList(oldTitle.getIdentifiersList());
    builder.contributorsList(oldTitle.getContributorsList());
    return builder.build();
  }

  private ResourcePut.ResourcePutBuilder convertCommonAttributesToResourcePutRequest(
    ResourcePutDataAttributes attributes, Title oldTitle) {
    var builder = ResourcePut.builder();
    var oldResource = oldTitle.getCustomerResourcesList().getFirst();
    builder.isSelected(valueOrDefault(attributes.getIsSelected(), oldResource.getIsSelected()));

    var proxy = attributes.getProxy();
    var proxyId = proxy != null && proxy.getId() != null ? proxy.getId() : oldResource.getProxy().getId();
    //RM API gives an error when we pass inherited as true along with updated proxy value
    //Hard code it to false; it should not affect the state of inherited that RM API maintains
    var rmApiProxy = org.folio.holdingsiq.model.Proxy.builder()
      .id(proxyId)
      .inherited(false)
      .build();
    builder.proxy(rmApiProxy);

    var oldHidden = oldResource.getVisibilityData() != null ? oldResource.getVisibilityData().getIsHidden() : null;
    var isHidden = attributes.getVisibilityData() != null && attributes.getVisibilityData().getIsHidden() != null
                   ? attributes.getVisibilityData().getIsHidden() : oldHidden;

    builder.isHidden(isHidden);
    if (Boolean.TRUE == oldResource.getIsPackageCustom()) {
      builder.url(valueOrDefault(attributes.getUrl(), oldResource.getUrl()));
    }

    var coverageStatement = valueOrDefault(attributes.getCoverageStatement(), oldResource.getCoverageStatement());
    builder.coverageStatement(coverageStatement);

    var embargoPeriod = attributes.getCustomEmbargoPeriod();
    if (embargoPeriod != null && embargoPeriod.getEmbargoUnit() != null
      && embargoPeriod.getEmbargoValue() != null && embargoPeriod.getEmbargoValue() > 0) {
      var customEmbargo = EmbargoPeriod.builder()
        .embargoUnit(embargoPeriod.getEmbargoUnit().value())
        .embargoValue(embargoPeriod.getEmbargoValue())
        .build();
      builder.customEmbargoPeriod(customEmbargo);
    }

    var coverageDates = attributes.getCustomCoverages() != null
                        ? convertToRmApiCustomCoverageList(attributes.getCustomCoverages())
                        : oldResource.getCustomCoverageList();
    builder.customCoverageList(coverageDates);

    // For now, we do not have any attributes specific to managed resources to be mapped to RM API fields
    // but below, we set the same values as we conduct a GET for pubType and isPeerReviewed because otherwise
    // RM API gives a bad request error if those values are set to null. All the other fields are retained as
    // is by RM API because they cannot be updated.
    builder.pubType(oldTitle.getPubType());
    builder.isPeerReviewed(oldTitle.getIsPeerReviewed());

    builder.userDefinedFields(UserDefinedFields.builder()
      .userDefinedField1(
        valueOrDefault(attributes.getUserDefinedField1(), oldResource.getUserDefinedFields().getUserDefinedField1()))
      .userDefinedField2(
        valueOrDefault(attributes.getUserDefinedField2(), oldResource.getUserDefinedFields().getUserDefinedField2()))
      .userDefinedField3(
        valueOrDefault(attributes.getUserDefinedField3(), oldResource.getUserDefinedFields().getUserDefinedField3()))
      .userDefinedField4(
        valueOrDefault(attributes.getUserDefinedField4(), oldResource.getUserDefinedFields().getUserDefinedField4()))
      .userDefinedField5(
        valueOrDefault(attributes.getUserDefinedField5(), oldResource.getUserDefinedFields().getUserDefinedField5()))
      .build());

    return builder;
  }

  private <T> T valueOrDefault(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  private List<CoverageDates> convertToRmApiCustomCoverageList(List<Coverage> customCoverages) {
    return mapItems(customCoverages,
      coverage -> CoverageDates.builder()
        .beginCoverage(coverage.getBeginCoverage())
        .endCoverage(coverage.getEndCoverage())
        .build());
  }
}
