package org.folio.util;

import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_VIEW_NAME;
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
import static org.folio.rest.impl.WireMockTestBase.JOHN_ID;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.converter.Converter;

import org.folio.db.DbUtils;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.AccessTypeMappingsTableConstants;
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
  public static final String KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT = KB_CREDENTIALS_ENDPOINT + "/%s/access-types/%s";

  private static final Converter<DbAccessType, AccessType> CONVERTER = new AccessTypeConverter.FromDb();

  public static void insertAccessTypeMapping(String recordId, RecordType recordType, String accessTypeId, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String query = prepareQuery(AccessTypeMappingsTableConstants.UPSERT_QUERY, accessTypesMappingTestTable());
    Tuple params = Tuple.of(UUID.randomUUID(), recordId, recordType.getValue(), toUUID(accessTypeId));

    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static List<AccessType> insertAccessTypes(List<AccessType> items, Vertx vertx) {
    for (AccessType item : items) {
      String id = insertAccessType(item, vertx);
      item.setId(id);
    }
    return items;
  }

  public static String insertAccessType(AccessType accessType, Vertx vertx) {
    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();

    String query = prepareQuery(UPSERT_ACCESS_TYPE_QUERY, accessTypesTestTable());

    UUID id = UUID.randomUUID();
    Tuple params = DbUtils.createParams(
      id,
      toUUID(accessType.getAttributes().getCredentialsId()),
      accessType.getAttributes().getName(),
      accessType.getAttributes().getDescription(),
      OffsetDateTime.now(),
      toUUID(JOHN_ID),
      null,
      null
    );

    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();

    return id.toString();
  }

  public static List<AccessTypeMapping> getAccessTypeMappings(Vertx vertx) {
    CompletableFuture<List<AccessTypeMapping>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), accessTypesMappingTestTable());
    PostgresClient.getInstance(vertx)
      .select(query, event -> future.complete(mapItems(event.result(), AccessTypesTestUtil::mapAccessTypeMapping)));
    return future.join();
  }

  public static List<AccessType> getAccessTypes(Vertx vertx) {
    CompletableFuture<List<AccessType>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), accessTypesTestView());
    PostgresClient.getInstance(vertx)
      .select(query, event -> future.complete(mapItems(event.result(), AccessTypesTestUtil::mapAccessType)));
    return future.join();
  }

  private static AccessTypeMapping mapAccessTypeMapping(Row entry) {
    return AccessTypeMapping.builder()
      .id(entry.getUUID(ID_COLUMN))
      .accessTypeId(entry.getUUID(ACCESS_TYPE_ID_COLUMN))
      .recordId(entry.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(entry.getString(RECORD_TYPE_COLUMN)))
      .build();
  }

  private static AccessType mapAccessType(Row resultRow) {
    return CONVERTER.convert(DbAccessType.builder()
      .id(resultRow.getUUID(ID_COLUMN))
      .credentialsId(resultRow.getUUID(CREDENTIALS_ID_COLUMN))
      .name(resultRow.getString(NAME_COLUMN))
      .description(resultRow.getString(DESCRIPTION_COLUMN))
      .usageNumber(ObjectUtils.defaultIfNull(resultRow.getInteger(USAGE_NUMBER_COLUMN), 0))
      .createdDate(resultRow.getOffsetDateTime(CREATED_DATE_COLUMN))
      .createdByUserId(resultRow.getUUID(CREATED_BY_USER_ID_COLUMN))
      .createdByUsername(resultRow.getString(CREATED_BY_USERNAME_COLUMN))
      .createdByLastName(resultRow.getString(CREATED_BY_LAST_NAME_COLUMN))
      .createdByFirstName(resultRow.getString(CREATED_BY_FIRST_NAME_COLUMN))
      .createdByMiddleName(resultRow.getString(CREATED_BY_MIDDLE_NAME_COLUMN))
      .updatedDate(resultRow.getOffsetDateTime(UPDATED_DATE_COLUMN))
      .updatedByUserId(resultRow.getUUID(UPDATED_BY_USER_ID_COLUMN))
      .updatedByUsername(resultRow.getString(UPDATED_BY_USERNAME_COLUMN))
      .updatedByLastName(resultRow.getString(UPDATED_BY_LAST_NAME_COLUMN))
      .updatedByFirstName(resultRow.getString(UPDATED_BY_FIRST_NAME_COLUMN))
      .updatedByMiddleName(resultRow.getString(UPDATED_BY_MIDDLE_NAME_COLUMN))
      .build());
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

  private static String accessTypesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_TABLE_NAME;
  }

  private static String accessTypesTestView() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_VIEW_NAME;
  }

  private static String accessTypesMappingTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_MAPPING_TABLE_NAME;
  }
}
