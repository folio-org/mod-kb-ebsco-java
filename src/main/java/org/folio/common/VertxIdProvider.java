package org.folio.common;

import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assigns UUID to specified Vertx instance if id isn't assigned already, and returns it from getVertxId().
 * Same Vertx instance will always have the same id, even if there are multiple instances of VertxIdProvider.
 */
@Component
public class VertxIdProvider {
  public static final String VERTX_ID_MAP = "vertxIdMap";
  public static final String VERTX_ID_KEY = "vertxId";
  private Vertx vertx;

  @Autowired
  public VertxIdProvider(Vertx vertx) {
    this.vertx = vertx;
    LocalMap<Object, Object> map = vertx.sharedData().getLocalMap(VERTX_ID_MAP);
    map.computeIfAbsent(VERTX_ID_KEY, key -> UUID.randomUUID().toString());
  }

  public String  getVertxId(){
    return (String) vertx.sharedData().getLocalMap(VERTX_ID_MAP).get(VERTX_ID_KEY);
  }
}

