package org.folio.rest.converter;

import java.util.Objects;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.TitleContributors;
import org.folio.rest.jaxrs.model.TitleIdentifier;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.apache.commons.lang3.StringUtils;
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
  
  private static final Map<Integer, String> IDENTIFIER_TYPES = new HashMap<>();
  static {
    IDENTIFIER_TYPES.put(0,"ISSN");
    IDENTIFIER_TYPES.put(1,"ISBN");
  }

  private static final Map<Integer, String> IDENTIFIER_SUBTYPES = new HashMap<>();
  static {
    IDENTIFIER_SUBTYPES.put(1,"Print");
    IDENTIFIER_SUBTYPES.put(2,"Online");
  }
  
  static final Map<String, PublicationType> publicationTypes = new HashMap<>();
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
  
  public List<TitleContributors> convertContributors(List<org.folio.rmapi.model.Contributor> contributorList) {
    return contributorList.stream().map(contributor ->
      new TitleContributors()
      .withContributor(contributor.getTitleContributor())
      .withType(StringUtils.capitalize(contributor.getType()))
      )
      .collect(Collectors.toList());
  }
  
  public List<TitleIdentifier> convertIdentifiers(List<Identifier> identifiersList) {
    return identifiersList.stream()
      .filter(identifier -> IDENTIFIER_TYPES.keySet().contains(identifier.getType()) && IDENTIFIER_SUBTYPES.keySet().contains(identifier.getSubtype()))
      .sorted(Comparator.comparing(Identifier::getType).thenComparing(Identifier::getSubtype))
      .map(identifier -> new TitleIdentifier()
                          .withId(identifier.getId())
                          .withType(IDENTIFIER_TYPES.getOrDefault(identifier.getType(), ""))
                          .withSubtype(IDENTIFIER_SUBTYPES.getOrDefault(identifier.getSubtype(), "")))
      .collect(Collectors.toList());
  }
  
  public List<TitleSubject> convertSubjects(List<Subject> subjectsList) {
    return subjectsList.stream().map(subject ->
      new TitleSubject()
      .withSubject(subject.getSubject())
      .withType(subject.getType())
      )
      .collect(Collectors.toList());
  }

  public org.folio.rest.jaxrs.model.EmbargoPeriod convertEmbargo(EmbargoPeriod customEmbargoPeriod) {
    org.folio.rest.jaxrs.model.EmbargoPeriod customEmbargo = new org.folio.rest.jaxrs.model.EmbargoPeriod();
    customEmbargo.setEmbargoUnit(customEmbargoPeriod.getEmbargoUnit());
    customEmbargo.setEmbargoValue(customEmbargoPeriod.getEmbargoValue());
    return customEmbargo;
  }

  public org.folio.rest.jaxrs.model.VisibilityData convertVisibilityData(VisibilityInfo visibilityData) {
    org.folio.rest.jaxrs.model.VisibilityData visibility = new org.folio.rest.jaxrs.model.VisibilityData();
    visibility.setIsHidden(visibilityData.getHidden());
    visibility.setReason(visibilityData.getReason().equals("Hidden by EP") ? "Set by system" : "");
    return visibility;
  }

  public List<Coverage> convertCoverages(List<CoverageDates> coverageList) {
    return coverageList.stream().map(coverageItem ->
    new Coverage()
      .withBeginCoverage(coverageItem.getBeginCoverage())
      .withEndCoverage(coverageItem.getEndCoverage())
    )
    .collect(Collectors.toList());
  }

  public org.folio.rest.jaxrs.model.Proxy convertProxy(org.folio.rmapi.model.Proxy proxy) {
    org.folio.rest.jaxrs.model.Proxy p = new org.folio.rest.jaxrs.model.Proxy();
    p.setId(proxy.getId());
    p.setInherited(proxy.getInherited());
    return p;
  }
}
