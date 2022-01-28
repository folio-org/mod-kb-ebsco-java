package org.folio.repository.users;

import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.common.LogUtils.logUpdateQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.users.UsersTableConstants.FIRST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.ID_COLUMN;
import static org.folio.repository.users.UsersTableConstants.LAST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.MIDDLE_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.PATRON_GROUP_COLUMN;
import static org.folio.repository.users.UsersTableConstants.USER_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.saveUserQuery;
import static org.folio.repository.users.UsersTableConstants.selectByIdQuery;
import static org.folio.repository.users.UsersTableConstants.updateUserQuery;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;

@Component
public class UsersRepositoryImpl implements UsersRepository {

  private static final Logger LOG = LogManager.getLogger(UsersRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Optional<DbUser>> findById(UUID id, String tenant) {
    String query = selectByIdQuery(tenant);
    Tuple params = createParams(id);

    logSelectQueryInfoLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).select(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleUser);
  }

  @Override
  public CompletableFuture<DbUser> save(DbUser user, String tenant) {
    String query = saveUserQuery(tenant);

    Tuple params = createParams(
      user.getId(),
      user.getUsername(),
      user.getFirstName(),
      user.getMiddleName(),
      user.getLastName(),
      user.getPatronGroup()
    );

    logInsertQueryInfoLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), updateResult -> user);
  }

  @Override
  public CompletableFuture<Boolean> update(DbUser user, String tenant) {
    String query = updateUserQuery(tenant);

    Tuple params = createParams(
      user.getUsername(),
      user.getFirstName(),
      user.getMiddleName(),
      user.getLastName(),
      user.getPatronGroup(),
      user.getId()
    );

    logUpdateQueryInfoLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()),
      rowSet -> rowSet.rowCount() > 0 ? Boolean.TRUE : Boolean.FALSE
    );
  }

  private Optional<DbUser> mapSingleUser(RowSet<Row> resultSet) {
    return RowSetUtils.isEmpty(resultSet)
      ? Optional.empty()
      : RowSetUtils.mapFirstItem(resultSet,
        row -> Optional.of(DbUser.builder()
          .id(row.getUUID(ID_COLUMN))
          .username(row.getString(USER_NAME_COLUMN))
          .lastName(row.getString(LAST_NAME_COLUMN))
          .firstName(row.getString(FIRST_NAME_COLUMN))
          .middleName(row.getString(MIDDLE_NAME_COLUMN))
          .patronGroup(row.getString(PATRON_GROUP_COLUMN))
          .build())
      );
  }
}
