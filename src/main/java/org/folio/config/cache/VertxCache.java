package org.folio.config.cache;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;

/**
 * Cache that stores values in vertx LocalMap
 * @param <T> Type of cached value
 */
public class VertxCache<T> {
  private Vertx vertx;
  private long expirationTime;
  private String mapKey;


  /**
   * @param vertx Vertx instance that will be used to store cache
   * @param expirationTime time of cache expiration in seconds,
   * @param vertxMapKey unique key that will be used to get LocalMap from vertx, VertxCache instances with same vertxMapKey
   *                    will have access to the same cache
   */
  @Autowired
  public VertxCache(Vertx vertx, long expirationTime, String vertxMapKey) {
    this.vertx = vertx;
    this.expirationTime = expirationTime;
    this.mapKey = vertxMapKey;
  }

  public T getValue(String key) {
    CacheWrapper<T> configurationWrapper = getLocalMap().computeIfPresent(key, (cacheKey, configuration) -> {
      if (LocalDateTime.now().isBefore(configuration.getExpireTime())) {
        return configuration;
      } else {
        return null;
      }
    });
    if (configurationWrapper == null) {
      return null;
    } else {
      return configurationWrapper.getCacheValue();
    }
  }

  public void putValue(String key, T cacheValue){
    LocalDateTime expireTime = LocalDateTime.now().plus(expirationTime, ChronoUnit.SECONDS);
    getLocalMap().put(key, new CacheWrapper<>(expireTime, cacheValue));
  }

  public void invalidate(String key){
    getLocalMap().remove(key);
  }

  private LocalMap<String, CacheWrapper<T>> getLocalMap() {
    return vertx.sharedData().getLocalMap(mapKey);
  }

  @lombok.Value
  private static class CacheWrapper<T> implements Shareable {
    private final LocalDateTime expireTime;
    private final T cacheValue;
  }
}
