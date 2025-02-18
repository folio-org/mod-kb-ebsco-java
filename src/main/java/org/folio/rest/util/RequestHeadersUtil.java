package org.folio.rest.util;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.NotAuthorizedException;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.tools.utils.TenantTool;

@UtilityClass
public class RequestHeadersUtil {

  /**
   * Retrieves tenant ID.
   *
   * @param headers HTTP headers to use, may be empty, but not null
   * @return the tenantId for the headers, returns the default "folio_shared" if undefined
   */
  public static String tenantId(Map<String, String> headers) {
    return TenantTool.tenantId(new CaseInsensitiveMap<>(headers));
  }

  public static String userId(Map<String, String> headers) {
    return new CaseInsensitiveMap<>(headers).get(XOkapiHeaders.USER_ID);
  }

  public static CompletableFuture<String> userIdFuture(Map<String, String> headers) {
    var userId = userId(headers);
    if (StringUtils.isBlank(userId)) {
      return CompletableFuture.failedFuture(
        new NotAuthorizedException(XOkapiHeaders.USER_ID + " header is required", StringUtils.defaultString(userId)));
    }
    return CompletableFuture.completedFuture(userId);
  }
}

