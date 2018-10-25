package org.folio.config.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.folio.config.RMAPIConfiguration;
import org.folio.properties.PropertyConfiguration;

import java.util.concurrent.TimeUnit;

public final class RMAPIConfigurationCache {
  private final Cache<String, RMAPIConfiguration> cache;

  private static final RMAPIConfigurationCache INSTANCE = new RMAPIConfigurationCache();
  private RMAPIConfigurationCache() {
    Long expirationTime = PropertyConfiguration.getInstance().getConfiguration().getLong("configuration.cache.expire");
    this.cache =  CacheBuilder.newBuilder()
      .expireAfterWrite(expirationTime, TimeUnit.SECONDS)
      .build();
  }

  public static RMAPIConfigurationCache getInstance(){
    return INSTANCE;
  }

  public RMAPIConfiguration getValue(String tenant){
    return cache.getIfPresent(tenant);
  }

  public void putValue(String tenant, RMAPIConfiguration configuration){
    cache.put(tenant, configuration);
  }

  public void invalidate(){
    cache.invalidateAll();
  }
}
