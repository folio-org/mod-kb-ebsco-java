package org.folio.rest.converter.titles;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.rest.jaxrs.model.RelatedTitle;
import org.folio.rest.jaxrs.model.TitleRelationship;
import org.jspecify.annotations.Nullable;

/**
 * Maps HoldingsIQ title relationships to the eHoldings API; they are present only when requested.
 */
public final class TitleRelationshipMapper {

  private TitleRelationshipMapper() {
  }

  public static @Nullable List<TitleRelationship> toApiRelationships(
    @Nullable List<org.folio.holdingsiq.model.TitleRelationship> relationships) {
    return relationships == null ? null : mapItems(relationships, TitleRelationshipMapper::toApiRelationship);
  }

  private static TitleRelationship toApiRelationship(org.folio.holdingsiq.model.TitleRelationship relationship) {
    return new TitleRelationship()
      .withId(relationship.resourceRelationshipId())
      .withType(relationship.type())
      .withDate(relationship.date())
      .withSource(toApiRelatedTitle(relationship.source()))
      .withTarget(toApiRelatedTitle(relationship.target()));
  }

  private static @Nullable RelatedTitle toApiRelatedTitle(
    org.folio.holdingsiq.model.@Nullable RelatedTitle relatedTitle) {
    return relatedTitle == null
           ? null
           : new RelatedTitle().withTitleId(relatedTitle.titleId()).withName(relatedTitle.name());
  }
}
