package org.folio.service.assignedusers;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.converter.assignedusers.UserCollectionDataConverter;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.service.users.Group;
import org.folio.service.users.User;
import org.folio.service.users.UsersLookUpService;

@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  private static final String IDS_NOT_MATCH_MESSAGE = "Credentials ID and user ID can't be updated";

  @Autowired
  private AssignedUserRepository assignedUserRepository;

  @Autowired
  private Converter<Collection<DbAssignedUser>, AssignedUserCollection> collectionConverter;
  @Autowired
  private Converter<UserCollectionDataConverter.UsersResult, AssignedUserCollection> userCollectionConverter;
  @Autowired
  private Converter<AssignedUserId, DbAssignedUser> toDbConverter;
  @Autowired
  private Converter<DbAssignedUser, AssignedUserId> toAssignedUserIdConverter;
  @Autowired
  private UsersLookUpService usersLookUpService;
  @Autowired
  private Converter<AssignedUserId, User> userConverter;

  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId,
                                                                       Map<String, String> okapiHeaders) {

    return assignedUserRepository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(dbAssignedUsers -> dbAssignedUsers.stream()
        .map(DbAssignedUser::getId)
        .collect(Collectors.toList()))
      .thenCompose(idBatches -> loadInBatches(idBatches,
        idBatch -> usersLookUpService.lookUpUsers(idBatch, new OkapiParams(okapiHeaders))))
      .thenCompose(users -> CompletableFuture.completedFuture(users)
        .thenCombine(fetchGroups(users, okapiHeaders), UserCollectionDataConverter.UsersResult::new)
        .thenApply(usersResult -> userCollectionConverter.convert(usersResult)));
  }

  @Override
  public CompletableFuture<AssignedUserId> save(AssignedUserPostRequest entity, Map<String, String> okapiHeaders) {
    AssignedUserId assignedUserId = entity.getData();
    return assignedUserRepository.save(toDbConverter.convert(assignedUserId), tenantId(okapiHeaders))
      .thenApply(source -> toAssignedUserIdConverter.convert(source));
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders) {
    return assignedUserRepository.delete(toUUID(credentialsId), toUUID(userId), tenantId(okapiHeaders));
  }

  private <T> CompletableFuture<Collection<T>> loadInBatches(List<UUID> ids,
                                                             Function<List<UUID>, CompletableFuture<Collection<T>>> loadFunction) {
    @SuppressWarnings("unchecked")
    CompletableFuture<Collection<T>>[] batchFutures = Lists.partition(ids, 50).stream()
      .map(loadFunction)
      .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(batchFutures)
      .thenCompose(v -> {
        Collection<T> resultCollection = new ArrayList<>();
        for (CompletableFuture<Collection<T>> future : batchFutures) {
          resultCollection.addAll(future.join());
        }
        return CompletableFuture.completedFuture(resultCollection);
      });
  }

  private CompletableFuture<Collection<Group>> fetchGroups(Collection<User> users, Map<String, String> okapiHeaders) {
    var groupIds = users.stream()
      .map(User::getPatronGroup)
      .filter(Objects::nonNull)
      .map(UUID::fromString)
      .distinct()
      .collect(Collectors.toList());
    return loadInBatches(groupIds,
      idBatch -> usersLookUpService.lookUpGroups(idBatch, new OkapiParams(okapiHeaders)));
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
