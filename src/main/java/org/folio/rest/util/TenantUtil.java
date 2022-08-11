package org.folio.rest.util;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.rest.tools.utils.TenantTool;

@UtilityClass
public class TenantUtil {

  /**
   * Retrieves tenant ID.
   *
   * @param headers HTTP headers to use, may be empty, but not null
   * @return the tenantId for the headers, returns the default "folio_shared" if undefined
   */
  public static String tenantId(Map<String, String> headers) {
    return TenantTool.tenantId(new CaseInsensitiveMap<>(headers));
  }
}

