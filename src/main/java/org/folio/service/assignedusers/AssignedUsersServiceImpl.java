package org.folio.service.assignedusers;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.AssignedUserPutRequest;

@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  private static final String IDS_NOT_MATCH_MESSAGE = "Credentials ID and user ID can't be updated";

  @Autowired
  private AssignedUserRepository repository;
  @Autowired
  private Converter<Collection<DbAssignedUser>, AssignedUserCollection> collectionConverter;
  @Autowired
  private Converter<AssignedUser, DbAssignedUser> toDbConverter;
  @Autowired
  private Converter<DbAssignedUser, AssignedUser> fromDbConverter;

  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId,
                                                                       Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(collectionConverter::convert);
  }

  @Override
  public CompletableFuture<AssignedUser> save(AssignedUserPostRequest entity, Map<String, String> okapiHeaders) {
    return repository.save(toDbConverter.convert(entity.getData()), tenantId(okapiHeaders))
      .thenApply(source -> fromDbConverter.convert(source));
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, String userId, AssignedUserPutRequest entity,
                                        Map<String, String> okapiHeaders) {
    AssignedUser assignedUser = entity.getData();
    return validate(credentialsId, userId, assignedUser)
      .thenCompose(o -> repository.update(toDbConverter.convert(assignedUser), tenantId(okapiHeaders)));
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders) {
    return repository.delete(toUUID(credentialsId), toUUID(userId), tenantId(okapiHeaders));
  }

  private CompletableFuture<Void> validate(String credentialsId, String userId, AssignedUser assignedUser) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    if (!assignedUser.getId().equals(userId)
      || !assignedUser.getAttributes().getCredentialsId().equals(credentialsId)) {
      BadRequestException exception = new BadRequestException(IDS_NOT_MATCH_MESSAGE);
      future.completeExceptionally(exception);
    } else {
      future.complete(null);
    }
    return future;
  }
}
