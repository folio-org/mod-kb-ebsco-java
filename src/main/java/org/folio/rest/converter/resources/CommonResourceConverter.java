package org.folio.rest.converter.resources;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.ProxyUrl;
import org.folio.holdingsiq.model.Subject;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VisibilityInfo;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.AlternateTitle;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CommonResourceConverter {

  private final Converter<List<Contributor>, List<Contributors>> contributorsConverter;
  private final Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
  private final Converter<List<Subject>, List<TitleSubject>> subjectsConverter;
  private final Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> embargoPeriodConverter;
  private final Converter<VisibilityInfo, VisibilityData> visibilityInfoConverter;
  private final Converter<List<CoverageDates>, List<Coverage>> coverageDatesConverter;
  private final Converter<ProxyUrl, org.folio.rest.jaxrs.model.ProxyUrl> proxyConverter;
  private final Converter<List<org.folio.holdingsiq.model.AlternateTitle>, List<AlternateTitle>>
    alternateTitleConverter;

  public CommonResourceConverter(
    Converter<List<Contributor>, List<Contributors>> contributorsConverter,
    Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter,
    Converter<List<Subject>, List<TitleSubject>> subjectsConverter,
    Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> embargoPeriodConverter,
    Converter<VisibilityInfo, VisibilityData> visibilityInfoConverter,
    Converter<List<CoverageDates>, List<Coverage>> coverageDatesConverter,
    Converter<ProxyUrl, org.folio.rest.jaxrs.model.ProxyUrl> proxyConverter,
    Converter<List<org.folio.holdingsiq.model.AlternateTitle>, List<AlternateTitle>> alternateTitleConverter) {
    this.contributorsConverter = contributorsConverter;
    this.identifiersConverter = identifiersConverter;
    this.subjectsConverter = subjectsConverter;
    this.embargoPeriodConverter = embargoPeriodConverter;
    this.visibilityInfoConverter = visibilityInfoConverter;
    this.coverageDatesConverter = coverageDatesConverter;
    this.proxyConverter = proxyConverter;
    this.alternateTitleConverter = alternateTitleConverter;
  }

  @SuppressWarnings("checkstyle:MethodLength")
  public ResourceDataAttributes createResourceDataAttributes(Title title, CustomerResources resource) {
    return new ResourceDataAttributes()
      .withAlternateTitles(alternateTitleConverter.convert(Objects.requireNonNullElse(title.getAlternateTitleList(),
        Collections.emptyList())))
      .withDescription(title.getDescription())
      .withEdition(title.getEdition())
      .withIsPeerReviewed(title.getIsPeerReviewed())
      .withIsTitleCustom(title.getIsTitleCustom())
      .withPublisherName(title.getPublisherName())
      .withTitleId(title.getTitleId())
      .withContributors(contributorsConverter.convert(title.getContributorsList()))
      .withIdentifiers(identifiersConverter.convert(title.getIdentifiersList()))
      .withName(title.getTitleName())
      .withPublicationType(ConverterConsts.PUBLICATION_TYPES.get(title.getPubType().toLowerCase()))
      .withSubjects(subjectsConverter.convert(title.getSubjectsList()))
      .withCoverageStatement(resource.getCoverageStatement())
      .withCustomEmbargoPeriod(embargoPeriodConverter.convert(resource.getCustomEmbargoPeriod()))
      .withIsPackageCustom(resource.getIsPackageCustom())
      .withIsSelected(resource.getIsSelected())
      .withIsTokenNeeded(resource.getIsTokenNeeded())
      .withLocationId(resource.getLocationId())
      .withManagedEmbargoPeriod(embargoPeriodConverter.convert(resource.getManagedEmbargoPeriod()))
      .withPackageId(resource.getVendorId() + "-" + resource.getPackageId())
      .withPackageName(resource.getPackageName())
      .withUrl(resource.getUrl())
      .withProviderId(resource.getVendorId())
      .withProviderName(resource.getVendorName())
      .withVisibilityData(visibilityInfoConverter.convert(resource.getVisibilityData()))
      .withManagedCoverages(coverageDatesConverter.convert(resource.getManagedCoverageList()))
      .withCustomCoverages(coverageDatesConverter.convert(
        resource.getCustomCoverageList().stream()
          .sorted(Comparator.comparing(CoverageDates::getBeginCoverage).reversed())
          .toList()))
      .withProxy(proxyConverter.convert(resource.getProxy()))
      .withUserDefinedField1(resource.getUserDefinedFields().getUserDefinedField1())
      .withUserDefinedField2(resource.getUserDefinedFields().getUserDefinedField2())
      .withUserDefinedField3(resource.getUserDefinedFields().getUserDefinedField3())
      .withUserDefinedField4(resource.getUserDefinedFields().getUserDefinedField4())
      .withUserDefinedField5(resource.getUserDefinedFields().getUserDefinedField5());
  }
}
