package org.folio.rest.converter.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rmapi.model.Contributor;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.EmbargoPeriod;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.Proxy;
import org.folio.rmapi.model.Subject;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.VisibilityInfo;

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
      .withCustomCoverages(coverageDatesConverter.convert(resource.getCustomCoverageList()))
      .withProxy(proxyConverter.convert(resource.getProxy()));
  }

}
