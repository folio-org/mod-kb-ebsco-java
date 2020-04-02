package org.folio.repository.kbcredentials;

import static java.lang.String.format;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.getKbCredentialsTableName;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.API_KEY_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_DATE_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CUSTOMER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.URL_COLUMN;

import java.util.Collection;
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
public class KbCredentialsRepositoryImpl implements KbCredentialsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(KbCredentialsRepositoryImpl.class);

  private static final String SELECT_LOG_MESSAGE = "Do select query = {}";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Collection<DbKbCredentials>> findAll(String tenant) {
    String query = format(SELECT_CREDENTIALS_QUERY, getKbCredentialsTableName(tenant));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenant).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCredentialsCollection);
  }

  private Collection<DbKbCredentials> mapCredentialsCollection(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapCredentials);
  }

  private DbKbCredentials mapCredentials(JsonObject row) {
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

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
