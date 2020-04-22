package org.folio.repository.accesstypes;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getAccessTypesOldTableName;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypesRepositoryImpl implements AccessTypesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesRepositoryImpl.class);

  private static final String LOG_SELECT_QUERY = "Do select query = {}";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;


  @Override
  public CompletableFuture<List<DbAccessType>> findByCredentialsId(String credentialsId, String tenantId) {
    String query = String.format(SELECT_BY_CREDENTIALS_ID_QUERY, getAccessTypesOldTableName(tenantId));

    LOG.info(LOG_SELECT_QUERY, tenantId);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, createParams(Collections.singleton(credentialsId)), promise);

    return mapResult(promise.future(), this::readAccessTypes);
  }

  private List<DbAccessType> readAccessTypes(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapAccessItem);
  }

  private DbAccessType mapAccessItem(JsonObject row) {
    return DbAccessType.builder()
      .id(row.getString(ID_COLUMN))
      .credentialsId(row.getString(CREDENTIALS_ID_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .description(row.getString(DESCRIPTION_COLUMN))
      .usageNumber(row.getInteger(USAGE_NUMBER_COLUMN))
      .createdDate(row.getInstant(CREATED_DATE_COLUMN))
      .createdByUserId(row.getString(CREATED_BY_USER_ID_COLUMN))
      .createdByUsername(row.getString(CREATED_BY_USERNAME_COLUMN))
      .createdByLastName(row.getString(CREATED_BY_LAST_NAME_COLUMN))
      .createdByFirstName(row.getString(CREATED_BY_FIRST_NAME_COLUMN))
      .createdByMiddleName(row.getString(CREATED_BY_MIDDLE_NAME_COLUMN))
      .updatedDate(row.getInstant(UPDATED_DATE_COLUMN))
      .updatedByUserId(row.getString(UPDATED_BY_USER_ID_COLUMN))
      .updatedByUsername(row.getString(UPDATED_BY_USERNAME_COLUMN))
      .updatedByLastName(row.getString(UPDATED_BY_LAST_NAME_COLUMN))
      .updatedByFirstName(row.getString(UPDATED_BY_FIRST_NAME_COLUMN))
      .updatedByMiddleName(row.getString(UPDATED_BY_MIDDLE_NAME_COLUMN))
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
