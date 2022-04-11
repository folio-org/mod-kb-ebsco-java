package org.folio.service.assignedusers;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.AssignedUserPutRequest;
import org.folio.service.users.Group;
import org.folio.service.users.User;
import org.folio.service.users.UsersLookUpService;
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
  private Converter<AssignedUser, DbAssignedUser> toDbConverter;
  @Autowired
  private Converter<DbAssignedUser, AssignedUser> fromDbConverter;
  @Autowired
  private Converter<AssignedUser, User> userConverter;
  @Autowired
  private UsersLookUpService usersLookUpService;

  @SneakyThrows
  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId,
                                                                       Map<String, String> okapiHeaders) {
    var dbAssignedUsers = assignedUserRepository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders));

    var ids = dbAssignedUsers
      .thenApply(dbAssignedUsers1 -> dbAssignedUsers1.stream()
        .map(DbAssignedUser::getId)
        .collect(Collectors.toList()));

    var users = new ArrayList<CompletableFuture<User>>();

    ids.thenApply(idList -> idList.stream().map(id -> {
      okapiHeaders.put(XOkapiHeaders.USER_ID, String.valueOf(id));
      return users.add(usersLookUpService.lookUpUser(new OkapiParams(okapiHeaders)));
    }));

    var groupIds = users.stream()
      .map(user -> user.thenApply(User::getPatronGroup)).collect(Collectors.toList());

    var groups = new CompletableFuture<ArrayList<Group>>();

    groupIds.forEach(groupId -> groupId.thenApply(id -> {
      okapiHeaders.put(XOkapiHeaders.USER_ID, id);
      return groups.thenApply(g -> {
        try {
          return g.add(usersLookUpService.lookUpGroup(new OkapiParams(okapiHeaders)).get());
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
          return null;
        }
      });
    }));

    var assignedUserCollection = dbAssignedUsers
      .thenApply(collectionConverter::convert);
    var c = new AtomicInteger();
    assignedUserCollection.thenCombine(groups,
      (assignedUserCollection1, groupsList) -> assignedUserCollection1.getData()
        .stream().peek(assignedUser ->
          assignedUser.getAttributes().setPatronGroup(groupsList.get(c.incrementAndGet()).getGroup())));

    return assignedUserCollection;
  }

  @Override
  public CompletableFuture<AssignedUser> save(AssignedUserPostRequest entity, Map<String, String> okapiHeaders) {
    AssignedUser assignedUser = entity.getData();
    return usersService.save(userConverter.convert(assignedUser), new OkapiParams(okapiHeaders))
      .thenCompose(user -> assignedUserRepository.save(toDbConverter.convert(assignedUser), tenantId(okapiHeaders)))
      .thenApply(source -> fromDbConverter.convert(source));
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, String userId, AssignedUserPutRequest entity,
                                        Map<String, String> okapiHeaders) {
    AssignedUser assignedUser = entity.getData();
    return validate(credentialsId, userId, assignedUser)
      .thenCompose(o -> usersService.update(userConverter.convert(assignedUser), new OkapiParams(okapiHeaders)));
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders) {
    return assignedUserRepository.delete(toUUID(credentialsId), toUUID(userId), tenantId(okapiHeaders));
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
