package org.folio.service.holdings;

public class HoldingConstants {
  public static final String LOAD_FACADE_ADDRESS = "load-service-facade.queue";
  public static final String HOLDINGS_SERVICE_ADDRESS = "holdings-service.queue";

  public static final String SAVE_HOLDINGS_ACTION = "saveHolding";
  public static final String SNAPSHOT_FAILED_ACTION = "snapshotFailed";
  public static final String SNAPSHOT_CREATED_ACTION = "snapshotCreated";
  public static final String CREATE_SNAPSHOT_ACTION = "createSnapshot";
  private HoldingConstants() {}
}
