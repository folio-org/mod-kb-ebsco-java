package org.folio.rest.converter.resources;

import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.ResourceRelationships;

public final class ResourceConverterUtils {

  private ResourceConverterUtils() {
  }

  public static ResourceRelationships createEmptyRelationship() {
    return new ResourceRelationships()
      .withProvider(new HasOneRelationship()
        .withMeta(
          new MetaDataIncluded()
            .withIncluded(false)))
      .withPackage(new HasOneRelationship().withMeta(
        new MetaDataIncluded()
          .withIncluded(false)))
      .withTitle(new HasOneRelationship().withMeta(
        new MetaDataIncluded()
          .withIncluded(false)));
  }
}
