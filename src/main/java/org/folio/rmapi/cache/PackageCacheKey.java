package org.folio.rmapi.cache;

import io.vertx.core.shareddata.Shareable;
import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.Configuration;

@Value
@Builder(toBuilder = true)
public class PackageCacheKey implements Shareable{
  private String packageId;
  private String tenant;
  private Configuration rmapiConfiguration;
}
