package org.folio.rest.converter.tags;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.util.RestConstants;


@Component
public class RectypeConverter implements Converter<String, org.folio.tag.RecordType> {

  private static final Map<String, org.folio.tag.RecordType> MAPPING = new HashMap<>();

  static {
    MAPPING.put(RestConstants.PROVIDER_RECTYPE, org.folio.tag.RecordType.PROVIDER);
    MAPPING.put(RestConstants.PACKAGE_RECTYPE, org.folio.tag.RecordType.PACKAGE);
    MAPPING.put(RestConstants.TITLE_RECTYPE, org.folio.tag.RecordType.TITLE);
    MAPPING.put(RestConstants.RESOURCE_RECTYPE, org.folio.tag.RecordType.RESOURCE);
  }

  @Override
  public org.folio.tag.RecordType convert(@NonNull String source) {
    org.folio.tag.RecordType result = MAPPING.get(source);

    if (result == null) {
      throw new IllegalArgumentException("Invalid record type: " + source);
    }

    return result;
  }

}
