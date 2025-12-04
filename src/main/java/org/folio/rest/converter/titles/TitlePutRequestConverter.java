package org.folio.rest.converter.titles;

import java.util.List;
import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.Proxy;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.rest.model.TitleCommonRequestAttributes;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitlePutRequestConverter {

  private final Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter;
  private final Converter<List<Contributors>, List<Contributor>> toContributorsConverter;

  @Autowired
  public TitlePutRequestConverter(
    Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> toIdentifiersConverter,
    Converter<List<Contributors>, List<Contributor>> toContributorsConverter) {
    this.toIdentifiersConverter = toIdentifiersConverter;
    this.toContributorsConverter = toContributorsConverter;
  }

  public ResourcePut convertToRmApiCustomResourcePutRequest(TitlePutRequest entity, CustomerResources oldResource) {
    ResourcePut.ResourcePutBuilder builder = ResourcePut.builder();

    builder.proxy(convertProxy(oldResource));
    builder.isHidden(oldResource.getVisibilityData().getIsHidden());
    builder.coverageStatement(oldResource.getCoverageStatement());
    builder.customEmbargoPeriod(oldResource.getCustomEmbargoPeriod());
    builder.customCoverageList(oldResource.getCustomCoverageList());
    builder.url(oldResource.getUrl());

    builder.isSelected(true);
    TitleCommonRequestAttributes attributes = entity.getData().getAttributes();
    if (attributes.getPublicationType() != null) {
      builder.pubType(ConverterConsts.PUBLICATION_TYPES.inverseBidiMap().get(attributes.getPublicationType()));
    }
    builder.isPeerReviewed(attributes.getIsPeerReviewed());
    builder.titleName(attributes.getName());
    builder.publisherName(attributes.getPublisherName());
    builder.edition(attributes.getEdition());
    builder.description(attributes.getDescription());
    builder.identifiersList(toIdentifiersConverter.convert(attributes.getIdentifiers()));
    builder.contributorsList(toContributorsConverter.convert(attributes.getContributors()));
    builder.userDefinedFields(convertUserDefinedFields(oldResource));
    return builder.build();
  }

  private @Nullable Proxy convertProxy(CustomerResources oldResource) {
    Proxy proxy = null;
    if (oldResource.getProxy() != null && oldResource.getProxy().getId() != null) {
      proxy = Proxy.builder()
        .inherited(false)
        .id(oldResource.getProxy().getId())
        .build();
    }
    return proxy;
  }

  private UserDefinedFields convertUserDefinedFields(CustomerResources oldResource) {
    return UserDefinedFields.builder()
      .userDefinedField1(oldResource.getUserDefinedFields().getUserDefinedField1())
      .userDefinedField2(oldResource.getUserDefinedFields().getUserDefinedField2())
      .userDefinedField3(oldResource.getUserDefinedFields().getUserDefinedField3())
      .userDefinedField4(oldResource.getUserDefinedFields().getUserDefinedField4())
      .userDefinedField5(oldResource.getUserDefinedFields().getUserDefinedField5())
      .build();
  }
}
