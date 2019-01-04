package org.folio.rest.converter.titles;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Resources;

public final class TitleConverterUtils {

  private TitleConverterUtils() {
  }

  public static Relationships createEmptyResourcesRelationships() {
    return new Relationships()
      .withResources(new Resources()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false))
        .withData(null));
  }
}
