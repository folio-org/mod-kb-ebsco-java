package org.folio.rest.converter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Data;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TitleAttributes;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleContributors;
import org.folio.rest.jaxrs.model.TitleIdentifier;
import org.folio.rest.jaxrs.model.TitleListDataAttributes;
import org.folio.rest.jaxrs.model.TitlePublicationType;
import org.folio.rest.jaxrs.model.TitleRelationship;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.Subject;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;

public class TitleConverter {

  private static final TitleRelationship EMPTY_RESOURCES_RELATIONSHIP = new TitleRelationship()
    .withResources(new MetaIncluded().withMeta(
      new MetaDataIncluded()
        .withIncluded(false)));
  
  private static final Map<String, TitlePublicationType> publicationTypes = new HashMap<>();
  static {
    publicationTypes.put("audiobook", TitlePublicationType.AUDIOBOOK);
    publicationTypes.put("book", TitlePublicationType.BOOK);
    publicationTypes.put("bookseries", TitlePublicationType.BOOK_SERIES);
    publicationTypes.put("database", TitlePublicationType.DATABASE);
    publicationTypes.put("journal", TitlePublicationType.JOURNAL);
    publicationTypes.put("newsletter", TitlePublicationType.NEWSLETTER);
    publicationTypes.put("newspaper", TitlePublicationType.NEWSPAPER);
    publicationTypes.put("proceedings", TitlePublicationType.PROCEEDINGS);
    publicationTypes.put("report", TitlePublicationType.REPORT);
    publicationTypes.put("streamingaudio", TitlePublicationType.STREAMING_AUDIO);
    publicationTypes.put("streamingvideo", TitlePublicationType.STREAMING_VIDEO);
    publicationTypes.put("thesisdissertation", TitlePublicationType.THESIS_DISSERTATION);
    publicationTypes.put("website", TitlePublicationType.WEBSITE);
    publicationTypes.put("unspecified", TitlePublicationType.UNSPECIFIED);
  }

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

  public TitleCollection convert(Titles titles) {
    List<org.folio.rest.jaxrs.model.Titles> titleList = titles.getTitleList().stream()
      .map(this::convertTitle)
      .collect(Collectors.toList());
    return new TitleCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(titleList);
  }

  public org.folio.rest.jaxrs.model.Title convertFromRMAPITitle(org.folio.rmapi.model.Title rmapiTitle) {
    return new org.folio.rest.jaxrs.model.Title()
        .withData(new Data()
            .withId(String.valueOf(rmapiTitle.getTitleId()))
            .withType("titles")
            .withAttributes(new TitleAttributes()
                .withName(rmapiTitle.getTitleName())
                .withPublisherName(rmapiTitle.getPublisherName())
                .withIsTitleCustom(rmapiTitle.getTitleCustom())
                .withPublicationType(publicationTypes.get(rmapiTitle.getPubType().toLowerCase()))
                .withSubjects(convertSubjects(rmapiTitle.getSubjectsList()))
                .withIdentifiers(convertIdentifiers(rmapiTitle.getIdentifiersList()))
                .withEdition(rmapiTitle.getEdition())
                .withContributors(convertContributors(rmapiTitle.getContributorsList()))
                .withDescription(rmapiTitle.getDescription())
                .withIsPeerReviewed(rmapiTitle.getPeerReviewed())
                )
            .withRelationships(EMPTY_RESOURCES_RELATIONSHIP)
            )
        .withIncluded(null)
        .withJsonapi(RestConstants.JSONAPI);
  }
  
  private org.folio.rest.jaxrs.model.Titles convertTitle(Title title) {
    return new org.folio.rest.jaxrs.model.Titles()
      .withId(String.valueOf(title.getTitleId()))
      .withRelationships(EMPTY_RESOURCES_RELATIONSHIP)
      .withType("titles")
      .withAttributes(new TitleListDataAttributes()
        .withName(title.getTitleName())
        .withPublisherName(title.getPublisherName())
        .withIsTitleCustom(title.getTitleCustom())
        .withPublicationType(publicationTypes.get(title.getPubType().toLowerCase()))
        .withSubjects(convertSubjects(title.getSubjectsList()))
        .withIdentifiers(convertIdentifiers(title.getIdentifiersList())));
  }

  private List<TitleSubject> convertSubjects(List<Subject> subjectsList) {
    return subjectsList.stream().map(subject ->
      new TitleSubject()
      .withSubject(subject.getSubject())
      .withType(subject.getType())
      )
      .collect(Collectors.toList());
  }

  private List<TitleIdentifier> convertIdentifiers(List<Identifier> identifiersList) {
    return identifiersList.stream()
      .filter(identifier -> IDENTIFIER_TYPES.keySet().contains(identifier.getType()) && IDENTIFIER_SUBTYPES.keySet().contains(identifier.getSubtype()))
      .sorted(Comparator.comparing(Identifier::getType).thenComparing(Identifier::getSubtype))
      .map(identifier -> new TitleIdentifier()
                          .withId(identifier.getId())
                          .withType(IDENTIFIER_TYPES.getOrDefault(identifier.getType(), ""))
                          .withSubtype(IDENTIFIER_SUBTYPES.getOrDefault(identifier.getSubtype(), "")))
      .collect(Collectors.toList());
  }
  
  private List<TitleContributors> convertContributors(List<org.folio.rmapi.model.Contributor> contributorList) {
    return contributorList.stream().map(contributor ->
      new TitleContributors()
      .withContributor(contributor.getTitleContributor())
      .withType(contributor.getType())
      )
      .collect(Collectors.toList());
  }

}
