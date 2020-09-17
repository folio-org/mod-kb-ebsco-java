package org.folio.repository.uc;

import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbMetadataUtil.mapMetadata;
import static org.folio.repository.DbUtil.getUCSettingsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.uc.UCSettingsTableConstants.CURRENCY_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.CUSTOMER_KEY_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.ID_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.KB_CREDENTIALS_ID_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.PLATFORM_TYPE_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.SELECT_UC_SETTINGS_BY_CREDENTIALS_ID;
import static org.folio.repository.uc.UCSettingsTableConstants.START_MONTH_COLUMN;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.DbUtil;

@Component
public class UCSettingsRepositoryImpl implements UCSettingsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(UCSettingsRepositoryImpl.class);

  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public UCSettingsRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Optional<DbUCSettings>> findByCredentialsId(UUID credentialsId, String tenant) {
    String query = prepareQuery(SELECT_UC_SETTINGS_BY_CREDENTIALS_ID, getUCSettingsTableName(tenant));
    Tuple params = createParams(credentialsId);

    logSelectQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    DbUtil.pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapUCSettings);
  }

  private Optional<DbUCSettings> mapUCSettings(RowSet<Row> rows) {
    return RowSetUtils.isEmpty(rows)
      ? Optional.empty()
      : RowSetUtils.mapFirstItem(rows, row -> {
        var builder = DbUCSettings.builder()
          .id(row.getUUID(ID_COLUMN))
          .kbCredentialsId(row.getUUID(KB_CREDENTIALS_ID_COLUMN))
          .customerKey(row.getString(CUSTOMER_KEY_COLUMN))
          .currency(row.getString(CURRENCY_COLUMN))
          .startMonth(row.getString(START_MONTH_COLUMN))
          .platformType(row.getString(PLATFORM_TYPE_COLUMN));
        return Optional.of(mapMetadata(builder, row).build());
      });
  }
}
