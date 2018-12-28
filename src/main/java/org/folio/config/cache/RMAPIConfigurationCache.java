package org.folio.config.cache;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.folio.config.RMAPIConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;

@Component
public class RMAPIConfigurationCache {
  private static final String CONFIGURATION_MAP_KEY = "configurationMap";

  private Vertx vertx;
  private long expirationTime;

  @Autowired
  public RMAPIConfigurationCache(Vertx vertx, @Value("${configuration.cache.expire}") long expirationTime) {
    this.vertx = vertx;
    this.expirationTime = expirationTime;
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
    return vertx.sharedData().getLocalMap(CONFIGURATION_MAP_KEY);
  }

  @lombok.Value
  private static class RMAPIConfigurationWrapper implements Shareable {
    private final LocalDateTime expireTime;
    private final RMAPIConfiguration rmapiConfiguration;
  }
}
