package org.folio.common;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assigns UUID to specified Vertx instance if id isn't assigned already, and returns it from getVertxId().
 * Same Vertx instance will always have the same id, even if there are multiple instances of VertxIdProvider.
 */
@Component
public class VertxIdProvider {

  private static final String VERTX_ID_MAP = "vertxIdMap";
  private static final String VERTX_ID_KEY = "vertxId";

  private final Vertx vertx;

  @Autowired
  public VertxIdProvider(Vertx vertx) {
    this.vertx = vertx;
    LocalMap<Object, Object> map = getLocalMap(vertx);
    map.computeIfAbsent(VERTX_ID_KEY, key -> UUID.randomUUID().toString());
  }

  public UUID getVertxId() {
    return UUID.fromString((String) getLocalMap(vertx).get(VERTX_ID_KEY));
  }

  private LocalMap<Object, Object> getLocalMap(Vertx vertx) {
    return vertx.sharedData().getLocalMap(VERTX_ID_MAP);
  }
}

