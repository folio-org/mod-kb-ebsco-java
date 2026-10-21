package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.Proxy;
import org.folio.holdingsiq.model.ProxyUrl;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ProxyDto;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ResourceRequestConverter {
  public ResourcePut convertToRmApiResourcePutRequest(ResourcePutRequest entity, Resource oldResource) {
    ResourcePutDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldResource);
    return builder.build();
  }

  public ResourcePut convertToRmApiCustomResourcePutRequest(ResourcePutRequest entity, Resource oldResource) {
    ResourcePutDataAttributes attributes = entity.getData().getAttributes();
    //Map common attributes for custom/managed resources to RM API fields
    ResourcePut.ResourcePutBuilder builder = convertCommonAttributesToResourcePutRequest(attributes, oldResource);
    //Map attributes specific to custom resources to RM API fields
    builder.titleName(oldResource.getTitleName());
    builder.publisherName(oldResource.getPublisherName());
    builder.edition(oldResource.getEdition());
    builder.description(oldResource.getDescription());

    builder.identifiersList(oldResource.getIdentifiersList());
    builder.contributorsList(oldResource.getContributorsList());
    return builder.build();
  }

  private ResourcePut.ResourcePutBuilder convertCommonAttributesToResourcePutRequest(
    ResourcePutDataAttributes attributes, Resource oldResource) {
    var builder = ResourcePut.builder();
    builder.isSelected(valueOrDefault(attributes.getIsSelected(), oldResource.getIsSelected()));

    convertProxy(builder, attributes.getProxy(), oldResource.getProxy());
    convertHidden(attributes, builder);
    if (Boolean.TRUE == oldResource.getIsPackageCustom()) {
      builder.url(valueOrDefault(attributes.getUrl(), oldResource.getUrl()));
    }

    var coverageStatement = valueOrDefault(attributes.getCoverageStatement(), oldResource.getCoverageStatement());
    builder.coverageStatement(coverageStatement);

    convertEmbargoPeriod(attributes, builder);
    convertCoverageDates(attributes, oldResource, builder);

    // For now, we do not have any attributes specific to managed resources to be mapped to RM API fields
    // but below, we set the same values as we conduct a GET for pubType and isPeerReviewed because otherwise
    // RM API gives a bad request error if those values are set to null. All the other fields are retained as
    // is by RM API because they cannot be updated.
    builder.pubType(oldResource.getPubType());
    builder.isPeerReviewed(oldResource.getIsPeerReviewed());

    convertUserDefinedFields(attributes, builder, oldResource);

    return builder;
  }

  /**
   * Sends visibility only when the request sets it; HoldingsIQ keeps the current visibility when it is omitted.
   * The per-category visibility takes precedence over the single visibility flag.
   */
  private void convertHidden(ResourcePutDataAttributes attributes, ResourcePut.ResourcePutBuilder builder) {
    var isHidden = attributes.getVisibilityData() != null ? attributes.getVisibilityData().getIsHidden() : null;
    builder.visibilityDetails(ResourceVisibility.toVisibilityDetails(attributes.getVisibility(), isHidden));
  }

  private void convertProxy(ResourcePut.ResourcePutBuilder builder, @Nullable ProxyDto newProxy, ProxyUrl oldProxy) {
    var proxyId = newProxy != null && newProxy.getId() != null ? newProxy.getId() : oldProxy.getId();
    //RM API gives an error when we pass inherited as true along with updated proxy value
    //Hard code it to false; it should not affect the state of inherited that RM API maintains
    var rmApiProxy = Proxy.builder()
      .id(proxyId)
      .inherited(false)
      .build();
    builder.proxy(rmApiProxy);
  }

  private void convertCoverageDates(ResourcePutDataAttributes attributes, Resource oldResource,
                                    ResourcePut.ResourcePutBuilder builder) {
    var coverageDates = attributes.getCustomCoverages() != null
                        ? convertToRmApiCustomCoverageList(attributes.getCustomCoverages())
                        : oldResource.getCustomCoverageList();
    builder.customCoverageList(coverageDates);
  }

  private void convertEmbargoPeriod(ResourcePutDataAttributes attributes, ResourcePut.ResourcePutBuilder builder) {
    var embargoPeriod = attributes.getCustomEmbargoPeriod();
    if (embargoPeriod != null && embargoPeriod.getEmbargoUnit() != null
        && embargoPeriod.getEmbargoValue() != null && embargoPeriod.getEmbargoValue() > 0) {
      var customEmbargo = EmbargoPeriod.builder()
        .embargoUnit(embargoPeriod.getEmbargoUnit().value())
        .embargoValue(embargoPeriod.getEmbargoValue())
        .build();
      builder.customEmbargoPeriod(customEmbargo);
    }
  }

  private void convertUserDefinedFields(ResourcePutDataAttributes attributes, ResourcePut.ResourcePutBuilder builder,
                                        Resource oldResource) {
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
  }

  private <T> T valueOrDefault(@Nullable T value, T defaultValue) {
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
