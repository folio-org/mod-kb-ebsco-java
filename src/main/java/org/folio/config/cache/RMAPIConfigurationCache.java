package org.folio.config.cache;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.folio.config.RMAPIConfiguration;
import org.folio.properties.PropertyConfiguration;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import lombok.Value;

public final class RMAPIConfigurationCache {
  private Vertx vertx;
  private long expirationTime;

  public RMAPIConfigurationCache(Vertx vertx) {
    this.expirationTime = PropertyConfiguration.getInstance().getConfiguration().getLong("configuration.cache.expire");
    this.vertx = vertx;
  }

  public RMAPIConfiguration getValue(String tenant) {
    RMAPIConfigurationWrapper configurationWrapper = getLocalMap().computeIfPresent(tenant, (key, configuration) -> {
      if (LocalDateTime.now().isBefore(configuration.getExpireTime())) {
        return configuration;
      } else {
        return null;
      }
    });
    if (configurationWrapper == null) {
      return null;
    } else {
      return configurationWrapper.getRmapiConfiguration();
    }
  }

  public void putValue(String tenant, RMAPIConfiguration configuration){
    LocalDateTime expireTime = LocalDateTime.now().plus(expirationTime, ChronoUnit.SECONDS);
    getLocalMap().put(tenant, new RMAPIConfigurationWrapper(expireTime, configuration));
  }

  public void invalidate(String tenant){
    getLocalMap().remove(tenant);
  }

  private LocalMap<String, RMAPIConfigurationWrapper> getLocalMap() {
    return vertx.sharedData().getLocalMap("configurationMap");
  }

  @Value
  private static class RMAPIConfigurationWrapper implements Shareable {
    private final LocalDateTime expireTime;
    private final RMAPIConfiguration rmapiConfiguration;
  }
}
