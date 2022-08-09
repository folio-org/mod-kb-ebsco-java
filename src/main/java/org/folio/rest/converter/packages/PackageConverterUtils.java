package org.folio.rest.converter.packages;

import static java.util.Collections.unmodifiableMap;

import java.util.EnumMap;
import java.util.Map;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.HasManyRelationship;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.PackageRelationship;

public final class PackageConverterUtils {

  public static final Map<ContentType, Integer> CONTENT_TYPE_TO_RMAPI_CODE;

  static {
    Map<ContentType, Integer> map = new EnumMap<>(ContentType.class);
    map.put(ContentType.AGGREGATED_FULL_TEXT, 1);
    map.put(ContentType.ABSTRACT_AND_INDEX, 2);
    map.put(ContentType.E_BOOK, 3);
    map.put(ContentType.E_JOURNAL, 4);
    map.put(ContentType.PRINT, 5);
    map.put(ContentType.UNKNOWN, 6);
    map.put(ContentType.ONLINE_REFERENCE, 7);
    map.put(ContentType.STREAMING_MEDIA, 8);
    map.put(ContentType.MIXED_CONTENT, 9);

    CONTENT_TYPE_TO_RMAPI_CODE = unmodifiableMap(map);
  }

  private PackageConverterUtils() {
  }

  public static PackageRelationship createEmptyPackageRelationship() {
    return new PackageRelationship()
      .withProvider(new HasOneRelationship()
        .withMeta(new MetaDataIncluded().withIncluded(false)))
      .withResources(new HasManyRelationship()
        .withMeta(new MetaDataIncluded()
          .withIncluded(false)));
  }

}
