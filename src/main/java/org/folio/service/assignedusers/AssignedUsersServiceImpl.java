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

import org.folio.common.OkapiParams;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.AssignedUserPutRequest;
import org.folio.service.users.User;
import org.folio.service.users.UsersService;

@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  private static final String IDS_NOT_MATCH_MESSAGE = "Credentials ID and user ID can't be updated";

  @Autowired
  private AssignedUserRepository assignedUserRepository;
  @Autowired
  private UsersService usersService;
  @Autowired
  private Converter<Collection<DbAssignedUser>, AssignedUserCollection> collectionConverter;
  @Autowired
  private Converter<AssignedUserId, DbAssignedUser> toDbConverter;
  @Autowired
  private Converter<DbAssignedUser, AssignedUserId> toAssignedUserIdConverter;
  @Autowired
  private Converter<AssignedUserId, User> userConverter;

  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId,
                                                                       Map<String, String> okapiHeaders) {
    return assignedUserRepository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(collectionConverter::convert);
  }

  @Override
  public CompletableFuture<AssignedUserId> save(AssignedUserPostRequest entity, Map<String, String> okapiHeaders) {
    AssignedUserId assignedUserId = entity.getData();
    return usersService.save(userConverter.convert(assignedUserId), new OkapiParams(okapiHeaders))
      .thenCompose(user -> assignedUserRepository.save(toDbConverter.convert(assignedUserId), tenantId(okapiHeaders)))
      .thenApply(source -> toAssignedUserIdConverter.convert(source));
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, String userId, AssignedUserPutRequest entity,
                                        Map<String, String> okapiHeaders) {
    AssignedUserId assignedUserId = entity.getData();
    return validate(userId, assignedUserId)
      .thenCompose(o -> usersService.update(userConverter.convert(assignedUserId), new OkapiParams(okapiHeaders)));
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders) {
    return assignedUserRepository.delete(toUUID(credentialsId), toUUID(userId), tenantId(okapiHeaders));
  }

  private CompletableFuture<Void> validate(String userId, AssignedUserId assignedUserId) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    if (!assignedUserId.getId().equals(userId)) {
      BadRequestException exception = new BadRequestException(IDS_NOT_MATCH_MESSAGE);
      future.completeExceptionally(exception);
    } else {
      future.complete(null);
    }
    return future;
  }
}
