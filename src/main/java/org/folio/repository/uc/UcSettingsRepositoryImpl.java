package org.folio.repository.uc;

import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbMetadataUtil.mapMetadata;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.uc.UcSettingsTableConstants.CURRENCY_COLUMN;
import static org.folio.repository.uc.UcSettingsTableConstants.CUSTOMER_KEY_COLUMN;
import static org.folio.repository.uc.UcSettingsTableConstants.ID_COLUMN;
import static org.folio.repository.uc.UcSettingsTableConstants.KB_CREDENTIALS_ID_COLUMN;
import static org.folio.repository.uc.UcSettingsTableConstants.PLATFORM_TYPE_COLUMN;
import static org.folio.repository.uc.UcSettingsTableConstants.START_MONTH_COLUMN;
import static org.folio.repository.uc.UcSettingsTableConstants.insertUcSettings;
import static org.folio.repository.uc.UcSettingsTableConstants.selectUcSettingsByCredentialsId;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.springframework.stereotype.Component;

@Component
public class UcSettingsRepositoryImpl implements UcSettingsRepository {

  private static final Logger LOG = LogManager.getLogger(UcSettingsRepositoryImpl.class);

  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public UcSettingsRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Optional<DbUcSettings>> findByCredentialsId(UUID credentialsId, String tenant) {
    String query = selectUcSettingsByCredentialsId(tenant);
    Tuple params = createParams(credentialsId);

    logSelectQueryInfoLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapUcSettings);
  }

  @Override
  public CompletableFuture<DbUcSettings> save(DbUcSettings ucSettings, String tenant) {
    String query = insertUcSettings(tenant);

    UUID id = ucSettings.getId() == null ? UUID.randomUUID() : ucSettings.getId();
    Tuple params = createParams(
      id,
      ucSettings.getKbCredentialsId(),
      ucSettings.getCustomerKey(),
      ucSettings.getCurrency(),
      ucSettings.getStartMonth(),
      ucSettings.getPlatformType(),
      ucSettings.getCreatedDate(),
      ucSettings.getCreatedByUserId(),
      ucSettings.getCreatedByUserName(),
      ucSettings.getUpdatedDate(),
      ucSettings.getUpdatedByUserId(),
      ucSettings.getUpdatedByUserName()
    );

    logInsertQueryInfoLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), setId(ucSettings, id));
  }

  private Optional<DbUcSettings> mapUcSettings(RowSet<Row> rows) {
    return RowSetUtils.isEmpty(rows)
           ? Optional.empty()
           : RowSetUtils.mapFirstItem(rows, row -> {
             var builder = DbUcSettings.builder()
               .id(row.getUUID(ID_COLUMN))
               .kbCredentialsId(row.getUUID(KB_CREDENTIALS_ID_COLUMN))
               .customerKey(row.getString(CUSTOMER_KEY_COLUMN))
               .currency(row.getString(CURRENCY_COLUMN))
               .startMonth(row.getString(START_MONTH_COLUMN))
               .platformType(row.getString(PLATFORM_TYPE_COLUMN));
             return Optional.of(mapMetadata(builder, row).build());
           });
  }

  private Function<RowSet<Row>, DbUcSettings> setId(DbUcSettings ucSettings, UUID id) {
    return updateResult -> ucSettings.toBuilder().id(id).build();
  }
}
