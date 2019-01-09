package org.folio.rest.converter.providers;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Relationships;

public final class ProviderConverterUtils {

  private ProviderConverterUtils() {
  }

  public static Relationships createEmptyProviderRelationships() {
    return new Relationships()
      .withPackages(new Packages()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false))
        .withData(null));
  }
}
