package org.folio.rest.converter.resources;

import java.util.Comparator;
import java.util.List;

import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Contributor;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.Identifier;
import org.folio.holdingsiq.model.Proxy;
import org.folio.holdingsiq.model.Subject;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VisibilityInfo;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.jaxrs.model.VisibilityData;

@Component
public class CommonResourceConverter {

  @Autowired
  private Converter<List<Contributor>, List<Contributors>> contributorsConverter;
  @Autowired
  private Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> identifiersConverter;
  @Autowired
  private Converter<List<Subject>, List<TitleSubject>> subjectsConverter;
  @Autowired
  private Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> embargoPeriodConverter;
  @Autowired
  private Converter<VisibilityInfo, VisibilityData> visibilityInfoConverter;
  @Autowired
  private Converter<List<CoverageDates>, List<Coverage>> coverageDatesConverter;
  @Autowired
  private Converter<Proxy, org.folio.rest.jaxrs.model.Proxy> proxyConverter;


  public ResourceDataAttributes createResourceDataAttributes(Title title, CustomerResources resource) {
    return new ResourceDataAttributes()
      .withDescription(title.getDescription())
      .withEdition(title.getEdition())
      .withIsPeerReviewed(title.getIsPeerReviewed())
      .withIsTitleCustom(title.getIsTitleCustom())
      .withPublisherName(title.getPublisherName())
      .withTitleId(title.getTitleId())
      .withContributors(contributorsConverter.convert(title.getContributorsList()))
      .withIdentifiers(identifiersConverter.convert(title.getIdentifiersList()))
      .withName(title.getTitleName())
      .withPublicationType(ConverterConsts.publicationTypes.get(title.getPubType().toLowerCase()))
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
        .collect(Collectors.toList())))
      .withProxy(proxyConverter.convert(resource.getProxy()));
  }

}
