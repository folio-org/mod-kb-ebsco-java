package org.folio.rest.model;

import java.util.List;
import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rest.jaxrs.model.Identifier;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.Tags;

public interface TitleCommonRequestAttributes {
  String getName();

  String getDescription();

  PublicationType getPublicationType();

  String getPublisherName();

  Boolean getIsPeerReviewed();

  String getEdition();

  List<Contributors> getContributors();

  List<Identifier> getIdentifiers();

  Tags getTags();
}
