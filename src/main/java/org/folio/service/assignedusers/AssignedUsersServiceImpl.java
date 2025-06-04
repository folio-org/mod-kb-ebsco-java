package org.folio.service.assignedusers;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.RequestHeadersUtil.tenantId;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.ws.rs.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.folio.common.OkapiParams;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.converter.assignedusers.UserCollectionDataConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.service.users.Group;
import org.folio.service.users.User;
import org.folio.service.users.UsersLookUpService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  private final AssignedUserRepository assignedUserRepository;

  private final Converter<UserCollectionDataConverter.UsersResult, AssignedUserCollection> userCollectionConverter;
  private final Converter<AssignedUserId, DbAssignedUser> toDbConverter;
  private final Converter<DbAssignedUser, AssignedUserId> toAssignedUserIdConverter;
  private final UsersLookUpService usersLookUpService;

  public AssignedUsersServiceImpl(
    AssignedUserRepository assignedUserRepository,
    Converter<UserCollectionDataConverter.UsersResult, AssignedUserCollection> userCollectionConverter,
    Converter<AssignedUserId, DbAssignedUser> toDbConverter,
    Converter<DbAssignedUser, AssignedUserId> toAssignedUserIdConverter,
    UsersLookUpService usersLookUpService) {
    this.assignedUserRepository = assignedUserRepository;
    this.userCollectionConverter = userCollectionConverter;
    this.toDbConverter = toDbConverter;
    this.toAssignedUserIdConverter = toAssignedUserIdConverter;
    this.usersLookUpService = usersLookUpService;
  }

  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId,
                                                                       Map<String, String> okapiHeaders) {
    String tenant = tenantId(okapiHeaders);
    log.debug("findByCredentialsId:: by [tenant: {}]", tenant);

    return assignedUserRepository.findByCredentialsId(toUUID(credentialsId), tenant)
      .thenApply(dbAssignedUsers -> dbAssignedUsers.stream()
        .map(DbAssignedUser::getId)
        .toList())
      .thenCompose(idBatches -> loadInBatches(idBatches,
        idBatch -> usersLookUpService.lookUpUsers(idBatch, new OkapiParams(okapiHeaders))))
      .thenCompose(users -> CompletableFuture.completedFuture(sortByLastName(users))
        .thenCombine(fetchGroups(users, okapiHeaders), UserCollectionDataConverter.UsersResult::new)
        .thenApply(userCollectionConverter::convert));
  }

  @Override
  public CompletableFuture<AssignedUserId> save(AssignedUserId assignedUserId, Map<String, String> okapiHeaders) {
    String tenantId = tenantId(okapiHeaders);
    log.debug("save:: by [assignedUserId: {}, tenant: {}]", assignedUserId, tenantId);

    return usersLookUpService.lookUpUserById(assignedUserId.getId(), new OkapiParams(okapiHeaders))
      .exceptionally(throwable -> {
          if (throwable instanceof NotFoundException) {
            throw new InputValidationException("Unable to assign user", "User doesn't exist");
          }
          throw new IllegalStateException("Unable to lookup user: " + throwable.getMessage());
        }
      ).thenCompose(u -> {
        log.info("save:: Attempts to save assignedUser by [tenant: {}]", tenantId);
        return assignedUserRepository.save(toDbConverter.convert(assignedUserId), tenantId)
          .thenApply(toAssignedUserIdConverter::convert);
      });
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders) {
    return assignedUserRepository.delete(toUUID(credentialsId), toUUID(userId), tenantId(okapiHeaders));
  }

  private List<User> sortByLastName(List<User> users) {
    users.sort(Comparator.comparing(User::getLastName));
    return users;
  }

  private <T> CompletableFuture<List<T>> loadInBatches(List<UUID> ids,
                                                       Function<List<UUID>, CompletableFuture<List<T>>> loadFunction) {
    @SuppressWarnings("unchecked")
    CompletableFuture<Collection<T>>[] batchFutures = Lists.partition(ids, 50).stream()
      .map(loadFunction)
      .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(batchFutures)
      .thenCompose(v -> {
        List<T> resultCollection = new ArrayList<>();
        for (CompletableFuture<Collection<T>> future : batchFutures) {
          resultCollection.addAll(future.join());
        }
        return CompletableFuture.completedFuture(resultCollection);
      });
  }

  private CompletableFuture<List<Group>> fetchGroups(Collection<User> users, Map<String, String> okapiHeaders) {
    var groupIds = users.stream()
      .map(User::getPatronGroup)
      .filter(Objects::nonNull)
      .map(UUID::fromString)
      .distinct()
      .toList();
    return loadInBatches(groupIds,
      idBatch -> usersLookUpService.lookUpGroups(idBatch, new OkapiParams(okapiHeaders)));
  }
}
