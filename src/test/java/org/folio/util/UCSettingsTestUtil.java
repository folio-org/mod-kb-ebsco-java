package org.folio.util;

import static org.folio.db.RowSetUtils.fromDate;
import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbMetadataUtil.mapMetadata;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.uc.UCSettingsTableConstants.CURRENCY_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.CUSTOMER_KEY_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.ID_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.INSERT_UC_SETTINGS;
import static org.folio.repository.uc.UCSettingsTableConstants.KB_CREDENTIALS_ID_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.PLATFORM_TYPE_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.START_MONTH_COLUMN;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.rest.impl.WireMockTestBase.JOHN_ID;
import static org.folio.rest.impl.WireMockTestBase.JOHN_USERNAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.springframework.core.convert.converter.Converter;

import org.folio.repository.uc.DbUCSettings;
import org.folio.rest.converter.uc.UCSettingsConverter;
import org.folio.rest.jaxrs.model.Meta;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.persist.PostgresClient;

public class UCSettingsTestUtil {

  public static final String UC_SETTINGS_ENDPOINT = "eholdings/kb-credentials/%s/uc";
  public static final String UC_SETTINGS_USER_ENDPOINT = "eholdings/uc";
  public static final String METRIC_TYPE_PARAM_TRUE = "?metrictype=true";

  public static final String STUB_CUSTOMER_KEY = "stub-customer-key";
  public static final String STUB_CURRENCY = "USD";
  public static final Month STUB_START_MONTH = Month.APR;
  public static final PlatformType STUB_PLATFORM_TYPE = PlatformType.ALL;

  private static final Converter<DbUCSettings, UCSettings> CONVERTER = new UCSettingsConverter.FromDbSecuredConverter();

  public static String saveUCSettings(UCSettings ucSettings, Vertx vertx) {
    var future = new CompletableFuture<>();

    var attributes = ucSettings.getAttributes();
    var meta = ucSettings.getMeta();

    var query = prepareQuery(INSERT_UC_SETTINGS, uCSettingsTestTable());
    UUID id = toUUID(ucSettings.getId());
    if (id == null) {
      id = UUID.randomUUID();
    }
    Tuple params = Tuple.of(
      id,
      toUUID(attributes.getCredentialsId()),
      attributes.getCustomerKey(),
      attributes.getCurrency(),
      attributes.getStartMonth().value(),
      attributes.getPlatformType().value(),
      fromDate(meta.getCreatedDate()),
      toUUID(meta.getCreatedByUserId()),
      meta.getCreatedByUsername(),
      fromDate(meta.getUpdatedDate()),
      toUUID(meta.getUpdatedByUserId()),
      meta.getUpdatedByUsername()
    );

    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();
    return fromUUID(id);
  }

  public static List<UCSettings> getUCSettings(Vertx vertx) {
    CompletableFuture<List<UCSettings>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), uCSettingsTestTable());
    PostgresClient.getInstance(vertx)
      .select(query, event -> future.complete(mapItems(event.result(), UCSettingsTestUtil::mapUCSettings)));
    return future.join();
  }

  private static UCSettings mapUCSettings(Row row) {
    var builder = DbUCSettings.builder()
      .id(row.getUUID(ID_COLUMN))
      .kbCredentialsId(row.getUUID(KB_CREDENTIALS_ID_COLUMN))
      .customerKey(row.getString(CUSTOMER_KEY_COLUMN))
      .currency(row.getString(CURRENCY_COLUMN))
      .startMonth(row.getString(START_MONTH_COLUMN))
      .platformType(row.getString(PLATFORM_TYPE_COLUMN));
    return CONVERTER.convert(mapMetadata(builder, row).build());
  }

  private static String uCSettingsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + UC_SETTINGS_TABLE_NAME;
  }

  public static UCSettings stubSettings(String credentialsId) {
    return new UCSettings()
      .withType(UCSettings.Type.UC_SETTINGS)
      .withAttributes(new UCSettingsDataAttributes()
        .withCredentialsId(credentialsId)
        .withCustomerKey(STUB_CUSTOMER_KEY)
        .withStartMonth(STUB_START_MONTH)
        .withPlatformType(STUB_PLATFORM_TYPE)
        .withCurrency(STUB_CURRENCY)
      )
      .withMeta(new Meta()
        .withCreatedDate(new Date())
        .withCreatedByUserId(JOHN_ID)
        .withCreatedByUsername(JOHN_USERNAME)
      );
  }
}
