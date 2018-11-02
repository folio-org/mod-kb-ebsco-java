package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleIdentifier;
import org.folio.rest.jaxrs.model.TitleListDataAttributes;
import org.folio.rest.jaxrs.model.TitleRelationship;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Identifier;
import org.folio.rmapi.model.Subject;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TitleConverter {

  private static final TitleRelationship EMPTY_RESOURCES_RELATIONSHIP = new TitleRelationship()
    .withResources(new MetaIncluded().withMeta(
      new MetaDataIncluded()
        .withIncluded(false)));

  private static final Map<String, TitleListDataAttributes.PublicationType> publicationTypes = new HashMap<>();
  static {
    publicationTypes.put("audiobook", TitleListDataAttributes.PublicationType.AUDIOBOOK);
    publicationTypes.put("book", TitleListDataAttributes.PublicationType.BOOK);
    publicationTypes.put("bookseries", TitleListDataAttributes.PublicationType.BOOK_SERIES);
    publicationTypes.put("database", TitleListDataAttributes.PublicationType.DATABASE);
    publicationTypes.put("journal", TitleListDataAttributes.PublicationType.JOURNAL);
    publicationTypes.put("newsletter", TitleListDataAttributes.PublicationType.NEWSLETTER);
    publicationTypes.put("newspaper", TitleListDataAttributes.PublicationType.NEWSPAPER);
    publicationTypes.put("proceedings", TitleListDataAttributes.PublicationType.PROCEEDINGS);
    publicationTypes.put("report", TitleListDataAttributes.PublicationType.REPORT);
    publicationTypes.put("streamingaudio", TitleListDataAttributes.PublicationType.STREAMING_AUDIO);
    publicationTypes.put("streamingvideo", TitleListDataAttributes.PublicationType.STREAMING_VIDEO);
    publicationTypes.put("thesisdissertation", TitleListDataAttributes.PublicationType.THESIS_DISSERTATION);
    publicationTypes.put("website", TitleListDataAttributes.PublicationType.WEBSITE);
    publicationTypes.put("unspecified", TitleListDataAttributes.PublicationType.UNSPECIFIED);
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

}
