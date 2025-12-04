package org.folio.rest.converter.tags;

import java.util.HashMap;
import java.util.Map;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RectypeConverter implements Converter<String, org.folio.repository.RecordType> {

  private static final Map<String, org.folio.repository.RecordType> MAPPING = new HashMap<>();

  static {
    MAPPING.put(RestConstants.PROVIDER_RECTYPE, org.folio.repository.RecordType.PROVIDER);
    MAPPING.put(RestConstants.PACKAGE_RECTYPE, org.folio.repository.RecordType.PACKAGE);
    MAPPING.put(RestConstants.TITLE_RECTYPE, org.folio.repository.RecordType.TITLE);
    MAPPING.put(RestConstants.RESOURCE_RECTYPE, org.folio.repository.RecordType.RESOURCE);
  }

  @Override
  public org.folio.repository.RecordType convert(String source) {
    org.folio.repository.RecordType result = MAPPING.get(source);

    if (result == null) {
      throw new IllegalArgumentException("Invalid record type: " + source);
    }

    return result;
  }
}
