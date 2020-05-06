package org.folio.repository.accesstypes;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME_OLD;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.NotFoundException;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;

@Component
public class AccessTypesOldRepositoryImpl implements AccessTypesOldRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesOldRepositoryImpl.class);

  private static final String ACCESS_TYPE_NOT_FOUND_MESSAGE = "Access type with id '%s' not found";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;


  @Override
  public CompletableFuture<List<AccessType>> findByNames(Collection<String> accessTypeNames, String tenantId) {
    Promise<Results<AccessType>> promise = Promise.promise();

    Criterion criterion = criterionForFindByNames(accessTypeNames);
    pgClient(tenantId).get(ACCESS_TYPES_TABLE_NAME_OLD, AccessType.class, criterion, false, false, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), Results::getResults);
  }

  @Override
  public CompletableFuture<Optional<AccessType>> findById(String id, String tenantId) {
    LOG.info("Retrieving access type: id = {}, tenantId = {}", id, tenantId);
    Promise<AccessType> promise = Promise.promise();
    pgClient(tenantId).getById(ACCESS_TYPES_TABLE_NAME_OLD, id, AccessType.class, promise);

    return mapResult(promise.future(), Optional::ofNullable);
  }

  @Override
  public CompletableFuture<Void> delete(String id, String tenantId) {
    Promise<UpdateResult> promise = Promise.promise();

    pgClient(tenantId).delete(ACCESS_TYPES_TABLE_NAME_OLD, id, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), updateResult -> {
      if (updateResult.getUpdated() == 0) {
        throw new NotFoundException(String.format(ACCESS_TYPE_NOT_FOUND_MESSAGE, id));
      }
      return null;
    });
  }

  private Criterion criterionForFindByNames(Collection<String> accessTypeNames) {
    Criterion criterion = new Criterion();
    accessTypeNames.forEach(name -> {
      Criteria criteria = new Criteria();
      criteria.addField("'attributes'");
      criteria.addField("'name'");
      criteria.setOperation("=");
      criteria.setVal(name);
      criterion.addCriterion(criteria, "OR");
    });
    return criterion;
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
