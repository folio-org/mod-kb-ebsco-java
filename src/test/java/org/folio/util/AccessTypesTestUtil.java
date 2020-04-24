package org.folio.util;

import static org.apache.commons.lang3.StringUtils.join;

import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_FIELD_LIST;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME_OLD;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_FIRST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_LAST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_MIDDLE_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_USERNAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.DESCRIPTION_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_FIRST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_LAST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_MIDDLE_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USERNAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPSERT_ACCESS_TYPE_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.USAGE_NUMBER_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.JSONB_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.repository.RecordType;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.converter.accesstypes.AccessTypeConverter;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
import org.folio.rest.persist.PostgresClient;

public class AccessTypesTestUtil {

  public static final String STUB_ACCESS_TYPE_NAME = "Subscribed";
  public static final String STUB_ACCESS_TYPE_NAME_2 = "Trial";
  public static final String STUB_ACCESS_TYPE_NAME_3 = "Purchased with perpetual access";

  public static final String ACCESS_TYPES_PATH = "/eholdings/access-types";
  public static final String KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT = KB_CREDENTIALS_ENDPOINT + "/%s/access-types";

  private static final Converter<DbAccessType, AccessType> CONVERTER = new AccessTypeConverter.FromDb();

  public static List<AccessType> getAccessTypesOld(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<AccessType>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select("SELECT * FROM " + accessTypesTestOldTable(),
      event -> future.complete(event.result().getRows().stream()
        .map(row -> row.getString(JSONB_COLUMN))
        .map(json -> parseAccessType(mapper, json))
        .collect(Collectors.toList())));
    return future.join();
  }

  public static List<AccessType> getAccessTypes(Vertx vertx) {
    CompletableFuture<List<AccessType>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(String.format(SqlQueryHelper.selectQuery(), accessTypesTestTable()),
      event -> future.complete(event.result().getRows().stream()
        .map(AccessTypesTestUtil::mapAccessType)
        .map(CONVERTER::convert)
        .collect(Collectors.toList())));
    return future.join();
  }

  public static void insertAccessTypeMapping(String recordId, final RecordType recordType, String accessTypeId,
                                             Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = "INSERT INTO " + accessTypesMappingTestTable() + "("
      + ACCESS_TYPES_MAPPING_FIELD_LIST + ") VALUES " + "(?,?,?,?)";
    JsonArray params =
      new JsonArray(Arrays.asList(UUID.randomUUID().toString(), recordId, recordType.getValue(), accessTypeId));

    PostgresClient.getInstance(vertx)
      .execute(insertStatement, params, event -> future.complete(null));
    future.join();
  }

  public static List<AccessType> insertAccessTypes(List<AccessType> items, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = "INSERT INTO " + accessTypesTestOldTable() +
      "(" + ID_COLUMN + "," + JSONB_COLUMN + ") VALUES " + join(Collections.nCopies(items.size(), "(?,?)"), ",") +
      " RETURNING " + ID_COLUMN;
    JsonArray params = createParams(items);

    PostgresClient.getInstance(vertx).select(insertStatement, params, ar -> {
      if (ar.succeeded()) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    });
    return populateAccessTypesIds(items, future.join().getRows());
  }

  public static String insertAccessType(AccessType accessType, Vertx vertx) {
    CompletableFuture<UpdateResult> future = new CompletableFuture<>();

    String query = String.format(UPSERT_ACCESS_TYPE_QUERY, accessTypesTestTable());

    String id = UUID.randomUUID().toString();
    JsonArray params = DbUtils.createParams(Arrays.asList(
      id,
      accessType.getAttributes().getCredentialsId(),
      accessType.getAttributes().getName(),
      accessType.getAttributes().getDescription(),
      Instant.now().toString(),
      UUID.randomUUID().toString(),
      "username",
      accessType.getCreator().getLastName(),
      accessType.getCreator().getFirstName(),
      accessType.getCreator().getMiddleName(),
      null, null, null, null, null, null
    ));

    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();

    return id;
  }

  public static List<AccessTypeMapping> getAccessTypeMappings(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    CompletableFuture<List<AccessTypeMapping>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select("SELECT * FROM " + accessTypesMappingTestTable(),
      event -> future.complete(event.result().getRows().stream()
        .map(entry -> parseAccessTypeMapping(mapper, entry))
        .collect(Collectors.toList())));
    return future.join();
  }

  private static AccessTypeMapping parseAccessTypeMapping(ObjectMapper mapper, JsonObject entry) {
    try {
      return mapper.readValue(entry.encode(), AccessTypeMapping.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse access type mapping", e);
    }
  }

  private static List<AccessType> populateAccessTypesIds(List<AccessType> items,
                                                         List<JsonObject> rows) {
    List<AccessType> result = new ArrayList<>(items.size());

    for (int i = 0; i < items.size(); i++) {
      result.add(items.get(i)
        .withId(rows.get(i).getString(ID_COLUMN)));
    }

    return result;
  }

  private static JsonArray createParams(List<AccessType> items) {
    JsonArray params = new JsonArray();

    items.forEach(item -> {
      params.add(UUID.randomUUID().toString());
      params.add(Json.encode(item));
    });

    return params;
  }

  private static AccessType parseAccessType(ObjectMapper mapper, String json) {
    try {
      return mapper.readValue(json, AccessType.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse access type", e);
    }
  }

  private static String accessTypesTestOldTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_TABLE_NAME_OLD;
  }

  private static String accessTypesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_TABLE_NAME;
  }

  private static String accessTypesMappingTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_MAPPING_TABLE_NAME;
  }

  private static DbAccessType mapAccessType(JsonObject resultRow) {
    return DbAccessType.builder()
      .id(resultRow.getString(ID_COLUMN))
      .credentialsId(resultRow.getString(CREDENTIALS_ID_COLUMN))
      .name(resultRow.getString(NAME_COLUMN))
      .description(resultRow.getString(DESCRIPTION_COLUMN))
      .usageNumber(ObjectUtils.defaultIfNull(resultRow.getInteger(USAGE_NUMBER_COLUMN), 0))
      .createdDate(resultRow.getInstant(CREATED_DATE_COLUMN))
      .createdByUserId(resultRow.getString(CREATED_BY_USER_ID_COLUMN))
      .createdByUsername(resultRow.getString(CREATED_BY_USERNAME_COLUMN))
      .createdByLastName(resultRow.getString(CREATED_BY_LAST_NAME_COLUMN))
      .createdByFirstName(resultRow.getString(CREATED_BY_FIRST_NAME_COLUMN))
      .createdByMiddleName(resultRow.getString(CREATED_BY_MIDDLE_NAME_COLUMN))
      .updatedDate(resultRow.getInstant(UPDATED_DATE_COLUMN))
      .updatedByUserId(resultRow.getString(UPDATED_BY_USER_ID_COLUMN))
      .updatedByUsername(resultRow.getString(UPDATED_BY_USERNAME_COLUMN))
      .updatedByLastName(resultRow.getString(UPDATED_BY_LAST_NAME_COLUMN))
      .updatedByFirstName(resultRow.getString(UPDATED_BY_FIRST_NAME_COLUMN))
      .updatedByMiddleName(resultRow.getString(UPDATED_BY_MIDDLE_NAME_COLUMN))
      .build();
  }

  public static List<AccessType> testData() {
    return testData(null);
  }

  public static List<AccessType> testData(String credentialsId) {
    AccessType accessType1 = new AccessType()
      .withType(AccessType.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withCredentialsId(credentialsId)
        .withName(STUB_ACCESS_TYPE_NAME)
        .withDescription("Access Type description 1"))
      .withCreator(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"));

    AccessType accessType2 = new AccessType()
      .withType(AccessType.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withCredentialsId(credentialsId)
        .withName(STUB_ACCESS_TYPE_NAME_2)
        .withDescription("Access Type description 2"))
      .withCreator(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"));

    AccessType accessType3 = new AccessType()
      .withType(AccessType.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withCredentialsId(credentialsId)
        .withName(STUB_ACCESS_TYPE_NAME_3)
        .withDescription("Access Type description 3"))
      .withCreator(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"));

    return Arrays.asList(accessType1, accessType2, accessType3);
  }
}
