package org.folio.service.assignedusers;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.DuplicateValueRepositoryException;
import org.folio.repository.ForeignKeyNotFoundRepositoryException;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.AssignedUserPutRequest;

@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  private static final String IDS_NOT_MATCH_MESSAGE = "Credentials ID and user ID can't be updated";
  private static final String USER_ASSIGN_NOT_ALLOWED_MESSAGE = "The user is already assigned to another credentials";

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
    return repository.findByCredentialsId(credentialsId, tenantId(okapiHeaders)).thenApply(collectionConverter::convert);
  }

  @Override
  public CompletableFuture<AssignedUser> save(AssignedUserPostRequest entity, Map<String, String> okapiHeaders) {
    CompletableFuture<AssignedUser> future = repository.save(toDbConverter.convert(entity.getData()), tenantId(okapiHeaders))
      .thenApply(source -> fromDbConverter.convert(source));
    return mapFutureWhenComplete(future);
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, String userId, AssignedUserPutRequest entity,
                                        Map<String, String> okapiHeaders) {
    AssignedUser assignedUser = entity.getData();

    CompletableFuture<Void> future = validate(credentialsId, userId, assignedUser)
      .thenCompose(o -> repository.update(toDbConverter.convert(assignedUser), tenantId(okapiHeaders)));
    return mapFutureWhenComplete(future);
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders) {
    CompletableFuture<Void> future = repository.delete(credentialsId, userId, tenantId(okapiHeaders));
    return mapFutureWhenComplete(future);
  }

  private CompletableFuture<Void> validate(String credentialsId, String userId, AssignedUser assignedUser) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    if (!assignedUser.getId().equals(userId) || !assignedUser.getAttributes().getCredentialsId().equals(credentialsId)) {
      BadRequestException exception = new BadRequestException(IDS_NOT_MATCH_MESSAGE);
      future.completeExceptionally(exception);
    } else {
      future.complete(null);
    }
    return future;
  }

  private <T> CompletableFuture<T> mapFutureWhenComplete(CompletableFuture<T> future) {
    CompletableFuture<T> resultFuture = new CompletableFuture<>();
    future.whenComplete(mapRepositoryException(resultFuture));
    return resultFuture;
  }

  private <T> BiConsumer<T, Throwable> mapRepositoryException(CompletableFuture<T> resultFuture) {
    return (assignedUser, throwable) -> {
      if (throwable != null) {
        Throwable cause = throwable.getCause();
        if (cause instanceof DuplicateValueRepositoryException) {
          cause = new BadRequestException(USER_ASSIGN_NOT_ALLOWED_MESSAGE);
        } else if (cause instanceof ForeignKeyNotFoundRepositoryException) {
          cause = new NotFoundException(cause.getMessage());
        }
        resultFuture.completeExceptionally(cause);
      } else {
        resultFuture.complete(assignedUser);
      }
    };
  }
}
