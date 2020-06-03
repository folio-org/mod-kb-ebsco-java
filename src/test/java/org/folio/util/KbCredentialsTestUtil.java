package org.folio.util;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.API_KEY_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_DATE_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CUSTOMER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPSERT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.URL_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.TokenTestUtils.generateToken;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.converter.kbcredentials.KbCredentialsConverter;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;

public class KbCredentialsTestUtil {

  public static final String KB_CREDENTIALS_ENDPOINT = "/eholdings/kb-credentials";
  public static final String USER_KB_CREDENTIAL_ENDPOINT = "/eholdings/user-kb-credential";
  public static final String KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT = KB_CREDENTIALS_ENDPOINT + "/%s/custom-labels";

  public static final String STUB_CREDENTIALS_NAME = "TEST_NAME";
  public static final String STUB_API_KEY = "TEST_API_KEY";
  public static final String STUB_API_URL = "https://api.ebsco.io";
  public static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";

  public static final String STUB_USERNAME = "TEST_USER_NAME";
  public static final String STUB_USERNAME_OTHER = "TEST_USER_NAME_OTHER";
  public static final String STUB_USER_ID = "88888888-8888-4888-8888-888888888888";
  public static final String STUB_USER_OTHER_ID = "99999999-9999-4999-9999-999999999999";

  public static final String STUB_INVALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9."
    + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.0ie9IdQ1KymERaS2hOENGsyzGcBiI7jsC-7XLcttcPs";

  public static final Header STUB_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN, generateToken(STUB_USERNAME,
      STUB_USER_ID));
  public static final Header STUB_TOKEN_OTHER_HEADER = new Header(XOkapiHeaders.TOKEN, generateToken(
      STUB_USERNAME_OTHER, STUB_USER_OTHER_ID));
  public static final Header STUB_INVALID_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN,
    KbCredentialsTestUtil.STUB_INVALID_TOKEN);

  private static final Converter<DbKbCredentials, KbCredentials> CONVERTER =
    new KbCredentialsConverter.KbCredentialsFromDbSecuredConverter(STUB_API_KEY);

  private static final Converter<DbKbCredentials, KbCredentials> CONVERTER_NON_SECURED =
    new KbCredentialsConverter.KbCredentialsFromDbNonSecuredConverter(STUB_API_KEY);

  public static String insertKbCredentials(String url, String name, String apiKey, String customerId, Vertx vertx) {
    return insertKbCredentials(UUID.randomUUID().toString(), url, name, apiKey, customerId, vertx);
  }

  public static String insertKbCredentials(String id, String url, String name, String apiKey, String customerId, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = String.format(UPSERT_CREDENTIALS_QUERY, kbCredentialsTestTable());
    JsonArray params = DbUtils.createParams(Arrays.asList(id, url, name, apiKey, customerId,
      Instant.now().toString(), STUB_USER_ID, STUB_USERNAME, null, null, null
    ));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static List<KbCredentials> getKbCredentials(Vertx vertx) {
    return getKbCredentials(vertx, CONVERTER);
  }

  public static List<KbCredentials> getKbCredentialsNonSecured(Vertx vertx) {
    return getKbCredentials(vertx, CONVERTER_NON_SECURED);
  }

  private static List<KbCredentials> getKbCredentials(Vertx vertx,
                                                      Converter<DbKbCredentials, KbCredentials> converter) {
    CompletableFuture<List<KbCredentials>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(String.format(SELECT_CREDENTIALS_QUERY, kbCredentialsTestTable()),
      event -> future.complete(event.result().getRows().stream()
        .map(KbCredentialsTestUtil::parseKbCredentials)
        .map(converter::convert)
        .collect(Collectors.toList())));
    return future.join();
  }

  private static DbKbCredentials parseKbCredentials(JsonObject row) {
    return DbKbCredentials.builder()
      .id(row.getString(ID_COLUMN))
      .url(row.getString(URL_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .apiKey(row.getString(API_KEY_COLUMN))
      .customerId(row.getString(CUSTOMER_ID_COLUMN))
      .createdDate(row.getInstant(CREATED_DATE_COLUMN))
      .updatedDate(row.getInstant(UPDATED_DATE_COLUMN))
      .createdByUserId(row.getString(CREATED_BY_USER_ID_COLUMN))
      .updatedByUserId(row.getString(UPDATED_BY_USER_ID_COLUMN))
      .createdByUserName(row.getString(CREATED_BY_USER_NAME_COLUMN))
      .updatedByUserName(row.getString(UPDATED_BY_USER_NAME_COLUMN))
      .build();
  }

  private static String kbCredentialsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + KB_CREDENTIALS_TABLE_NAME;
  }

  public static DbKbCredentials getCredentialsNoUrl() {
    return DbKbCredentials.builder()
      .name(STUB_CREDENTIALS_NAME)
      .customerId(STUB_CUSTOMER_ID)
      .apiKey(STUB_API_KEY).build();
  }
  public static DbKbCredentials getCredentials() {
    return DbKbCredentials.builder()
      .name(STUB_CREDENTIALS_NAME)
      .customerId(STUB_CUSTOMER_ID)
      .apiKey(STUB_API_KEY)
      .url(STUB_API_URL).build();
  }
}
