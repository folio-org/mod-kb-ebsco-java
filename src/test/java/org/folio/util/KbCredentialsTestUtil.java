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

  public static final String STUB_API_KEY = "TEST_API_KEY";
  public static final String STUB_USERNAME = "TEST_USER_NAME";
  public static final String STUB_USER_ID = "88888888-8888-4888-8888-888888888888";
  public static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  public static final String STUB_API_URL = "http://api.url.com";
  public static final String STUB_CREDENTIALS_NAME = "TEST_NAME";
  public static final String STUB_TOKEN = "eyJhbGciOiJIUzI1NiJ9."
    + "eyJzdWIiOiJURVNUX1VTRVJfTkFNRSIsInVzZXJfaWQiOiI4ODg4ODg4OC04ODg4LTQ4ODgtODg4OC04O"
    + "Dg4ODg4ODg4ODgiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImRpa3UifQ.0ie9IdQ1KymERaS2hOENGsyzGcBiI7jsC-7XLcttcPs";


  public static final Header STUB_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN, KbCredentialsTestUtil.STUB_TOKEN);

  private static final Converter<DbKbCredentials, KbCredentials> CONVERTER =
    new KbCredentialsConverter.KbCredentialsFromDbConverter(STUB_API_KEY);

  public static String insertKbCredentials(String url, String name, String apiKey, String customerId, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String id = UUID.randomUUID().toString();
    String insertStatement = String.format(UPSERT_CREDENTIALS_QUERY, kbCredentialsTestTable());
    JsonArray params = DbUtils.createParams(Arrays.asList(id, url, name, apiKey, customerId,
      Instant.now().toString(), STUB_USER_ID, STUB_USERNAME, null, null, null
    ));

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static List<KbCredentials> getKbCredentials(Vertx vertx) {
    CompletableFuture<List<KbCredentials>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(String.format(SELECT_CREDENTIALS_QUERY, kbCredentialsTestTable()),
      event -> future.complete(event.result().getRows().stream()
        .map(KbCredentialsTestUtil::parseKbCredentials)
        .map(CONVERTER::convert)
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
}
