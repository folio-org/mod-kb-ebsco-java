package org.folio.config.cache;

import io.vertx.core.shareddata.Shareable;
import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.Configuration;

@Value
@Builder(toBuilder = true)
public class VendorIdCacheKey implements Shareable {
  private String tenant;
  private Configuration rmapiConfiguration;
}
