package org.folio.rmapi.cache;

import io.vertx.core.shareddata.Shareable;
import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.ResourceId;

@Value
@Builder(toBuilder = true)
public class ResourceCacheKey implements Shareable {
  private ResourceId resourceId;
  private String tenant;
  private Configuration rmapiConfiguration;
}
