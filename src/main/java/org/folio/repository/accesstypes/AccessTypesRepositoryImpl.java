package org.folio.repository.accesstypes;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
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
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_BY_CREDENTIALS_ID_QUERY;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_FIRST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_LAST_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_MIDDLE_NAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USERNAME_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.USAGE_NUMBER_COLUMN;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypesRepositoryImpl implements AccessTypesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesRepositoryImpl.class);

  private static final String LOG_SELECT_QUERY = "Do select query = {}";

  @Autowired
  private DBExceptionTranslator excTranslator;

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsId(String credentialsId, String tenantId) {
    String query = String.format(SELECT_BY_CREDENTIALS_ID_QUERY,
      getAccessTypesTableName(tenantId), getAccessTypesMappingTableName(tenantId));

    LOG.info(LOG_SELECT_QUERY, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future(), this::readAccessTypes);
  }

  private List<DbAccessType> readAccessTypes(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapAccessType);
  }

  private DbAccessType mapAccessType(JsonObject resultRow) {
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

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
