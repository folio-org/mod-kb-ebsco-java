package org.folio.config.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.folio.config.RMAPIConfiguration;

import java.util.concurrent.TimeUnit;

public final class RMAPIConfigurationCache {
  private static final String CACHE_KEY = "Configuration";
  private final Cache<String, RMAPIConfiguration> cache;

  private static final RMAPIConfigurationCache INSTANCE = new RMAPIConfigurationCache();
  private RMAPIConfigurationCache() {
    this.cache =  CacheBuilder.newBuilder()
      .expireAfterWrite(2, TimeUnit.MINUTES)
      .build();
  }

  public static RMAPIConfigurationCache getInstance(){
    return INSTANCE;
  }

  public RMAPIConfiguration getValue(){
    return cache.getIfPresent(CACHE_KEY);
  }

  public void putValue(RMAPIConfiguration configuration){
    cache.put(CACHE_KEY, configuration);
  }
}
