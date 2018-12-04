package org.folio.rest.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.lang.StringUtils;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.Identifier.Subtype;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.EmbargoPeriod;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.Subject;
import org.folio.rmapi.model.VisibilityInfo;
import org.folio.rmapi.model.TokenInfo;

public class CommonAttributesConverter {

  private static final BidiMap<Integer, org.folio.rest.jaxrs.model.Identifier.Type> IDENTIFIER_TYPES = new TreeBidiMap<>();
  static {
    IDENTIFIER_TYPES.put(0, org.folio.rest.jaxrs.model.Identifier.Type.ISSN);
    IDENTIFIER_TYPES.put(1, org.folio.rest.jaxrs.model.Identifier.Type.ISBN);
  }

  private static final BidiMap<Integer, org.folio.rest.jaxrs.model.Identifier.Subtype> IDENTIFIER_SUBTYPES = new TreeBidiMap<>();
  static {
    IDENTIFIER_SUBTYPES.put(1, Subtype.PRINT);
    IDENTIFIER_SUBTYPES.put(2, Subtype.ONLINE);
  }

  private static final Map<String, org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit> EMBARGO_UNITS = new HashMap<>();
  static {
    EMBARGO_UNITS.put("Days", EmbargoUnit.DAYS);
    EMBARGO_UNITS.put("Weeks", EmbargoUnit.WEEKS);
    EMBARGO_UNITS.put("Months", EmbargoUnit.MONTHS);
    EMBARGO_UNITS.put("Years", EmbargoUnit.YEARS);
  }
  
  static final BidiMap<String, PublicationType> publicationTypes = new TreeBidiMap<>();
  static {
    publicationTypes.put("audiobook", PublicationType.AUDIOBOOK);
    publicationTypes.put("book", PublicationType.BOOK);
    publicationTypes.put("bookseries", PublicationType.BOOK_SERIES);
    publicationTypes.put("database", PublicationType.DATABASE);
    publicationTypes.put("journal", PublicationType.JOURNAL);
    publicationTypes.put("newsletter", PublicationType.NEWSLETTER);
    publicationTypes.put("newspaper", PublicationType.NEWSPAPER);
    publicationTypes.put("proceedings", PublicationType.PROCEEDINGS);
    publicationTypes.put("report", PublicationType.REPORT);
    publicationTypes.put("streamingaudio", PublicationType.STREAMING_AUDIO);
    publicationTypes.put("streamingvideo", PublicationType.STREAMING_VIDEO);
    publicationTypes.put("thesisdissertation", PublicationType.THESIS_DISSERTATION);
    publicationTypes.put("website", PublicationType.WEBSITE);
    publicationTypes.put("unspecified", PublicationType.UNSPECIFIED);
  }

  public Token convertToken(TokenInfo tokenInfo) {
    if(Objects.isNull(tokenInfo)){
      return null;
    }
    return new Token()
      .withFactName(tokenInfo.getFactName())
      .withHelpText(tokenInfo.getHelpText())
      .withPrompt(tokenInfo.getPrompt())
      .withValue(tokenInfo.getValue() == null ? null : (String) tokenInfo.getValue());
  }

  public List<org.folio.rest.jaxrs.model.Contributors> convertContributors(List<org.folio.rmapi.model.Contributor> contributorList) {
      if(Objects.isNull(contributorList)) {
        return Collections.emptyList();
      }
    return contributorList.stream().map(contributor ->
      new org.folio.rest.jaxrs.model.Contributors()
      .withContributor(contributor.getTitleContributor())
      .withType(StringUtils.capitalize(contributor.getType()))
      )
      .collect(Collectors.toList());
  }

  public List<org.folio.rmapi.model.Contributor> convertToContributors(List<org.folio.rest.jaxrs.model.Contributors> contributorList) {

      return contributorList.stream().map(contributor ->
      org.folio.rmapi.model.Contributor.builder()
        .titleContributor(contributor.getContributor())
        .type(StringUtils.capitalize(contributor.getType())).build()
    )
      .collect(Collectors.toList());
  }

  public List<org.folio.rest.jaxrs.model.Identifier> convertIdentifiers(List<Identifier> identifiersList) {
    if (Objects.isNull(identifiersList)) {
      return Collections.emptyList();
    }
    return identifiersList.stream()
      .filter(identifier -> IDENTIFIER_TYPES.keySet().contains(identifier.getType()) && IDENTIFIER_SUBTYPES.keySet().contains(identifier.getSubtype()))
      .sorted(Comparator.comparing(Identifier::getType).thenComparing(Identifier::getSubtype))
      .map(identifier -> new org.folio.rest.jaxrs.model.Identifier()
        .withId(identifier.getId())
        .withType(IDENTIFIER_TYPES
          .getOrDefault(identifier.getType(), org.folio.rest.jaxrs.model.Identifier.Type.ISBN))
        .withSubtype(IDENTIFIER_SUBTYPES.getOrDefault(identifier.getSubtype(), Subtype.ONLINE)))
      .collect(Collectors.toList());
  }
  public List<Identifier> convertToIdentifiers(List<org.folio.rest.jaxrs.model.Identifier> identifiersList) {

    return identifiersList.stream()
      .sorted(Comparator.comparing(org.folio.rest.jaxrs.model.Identifier::getSubtype).thenComparing(org.folio.rest.jaxrs.model.Identifier::getType))
      .map(identifier ->  org.folio.rmapi.model.Identifier.builder()
        .id(identifier.getId())
        .type(IDENTIFIER_TYPES.inverseBidiMap().get(identifier.getType()))
        .subtype(IDENTIFIER_SUBTYPES.inverseBidiMap().get(identifier.getSubtype()))
        .build())
      .collect(Collectors.toList());
  }


  public List<TitleSubject> convertSubjects(List<Subject> subjectsList) {
    if(Objects.isNull(subjectsList)) {
      return Collections.emptyList();
    }
    return subjectsList.stream().map(subject ->
      new TitleSubject()
      .withSubject(subject.getValue())
      .withType(subject.getType())
      )
      .collect(Collectors.toList());
  }

  public org.folio.rest.jaxrs.model.EmbargoPeriod convertEmbargo(EmbargoPeriod customEmbargoPeriod) {
    if(Objects.isNull(customEmbargoPeriod)){
      return null;
    }
    org.folio.rest.jaxrs.model.EmbargoPeriod customEmbargo = new org.folio.rest.jaxrs.model.EmbargoPeriod();
    customEmbargo.setEmbargoUnit(EMBARGO_UNITS.get(customEmbargoPeriod.getEmbargoUnit()));
    customEmbargo.setEmbargoValue(customEmbargoPeriod.getEmbargoValue());
    return customEmbargo;
  }

  public org.folio.rest.jaxrs.model.VisibilityData convertVisibilityData(VisibilityInfo visibilityData) {
    
    if(Objects.isNull(visibilityData)){
      return null;
    }
    org.folio.rest.jaxrs.model.VisibilityData visibility = new org.folio.rest.jaxrs.model.VisibilityData();
    visibility.setIsHidden(visibilityData.getIsHidden());
    visibility.setReason(visibilityData.getReason().equals("Hidden by EP") ? "Set by system" : "");
    return visibility;
  }

  public List<Coverage> convertCoverages(List<CoverageDates> coverageList) {
    if(Objects.isNull(coverageList)) {
      return Collections.emptyList();
    }
    return coverageList.stream().map(coverageItem ->
    new Coverage()
      .withBeginCoverage(coverageItem.getBeginCoverage())
      .withEndCoverage(coverageItem.getEndCoverage())
    )
    .collect(Collectors.toList());
  }

  public org.folio.rest.jaxrs.model.Proxy convertProxy(org.folio.rmapi.model.Proxy proxy) {
    
    if(Objects.isNull(proxy)){
      return null;
    }
    org.folio.rest.jaxrs.model.Proxy p = new org.folio.rest.jaxrs.model.Proxy();
    p.setId(proxy.getId());
    p.setInherited(proxy.getInherited());
    return p;
  }
}
