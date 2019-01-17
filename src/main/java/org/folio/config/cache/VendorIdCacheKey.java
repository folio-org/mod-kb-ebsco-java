package org.folio.config.cache;

import org.folio.config.RMAPIConfiguration;

import io.vertx.core.shareddata.Shareable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class VendorIdCacheKey implements Shareable {
  private String tenant;
  private RMAPIConfiguration rmapiConfiguration;
}
