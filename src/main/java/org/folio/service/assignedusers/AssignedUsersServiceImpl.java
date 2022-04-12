package org.folio.service.assignedusers;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import com.google.common.collect.Iterables;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
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
import org.folio.util.StringUtil;

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
  private Converter<Collection<User>, AssignedUserCollection> userCollectionConverter;
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

    var ids = assignedUserRepository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(dbAssignedUsers1 -> dbAssignedUsers1.stream()
        .map(DbAssignedUser::getId)
        .collect(Collectors.toList()));

    var users = new ArrayList<User>();
    var usersIds = new ArrayList<String>();
    var usersBatch = new ArrayList<CompletableFuture<Collection<User>>>();

    var patronGroupIds = new ArrayList<String>();
    var groupIds = new ArrayList<String>();
    var groupsBatch = new ArrayList<CompletableFuture<Collection<Group>>>();
    var groups = new ArrayList<Group>();
    var usersList = ids.thenApply(uuids -> uuids.stream()
      .map(String::valueOf).collect(Collectors.toList()))
      .thenApply(idList -> {
        Iterables.partition(idList, 50).forEach(strings -> {
          String idsCql = "id=(" + strings.stream().map(StringUtil::cqlEncode).collect(Collectors.joining(" OR ")) + ")";
          usersIds.add(idsCql);
        });
        return usersIds;
      });

    usersIds.forEach(query -> {
      usersBatch.add(usersLookUpService.lookUpUsersUsingCQL(new OkapiParams(okapiHeaders), query));
    });

    usersBatch.forEach(collectionCompletableFuture ->
        collectionCompletableFuture
          .thenApply(userCollection -> {
            userCollection.forEach(user -> patronGroupIds.add(user.getPatronGroup()));
            return patronGroupIds;
          })
          .thenApply(idList -> {
            Iterables.partition(idList, 50).forEach(strings -> {
              String idsCql = "id=(" + strings.stream().map(StringUtil::cqlEncode).collect(Collectors.joining(" OR ")) + ")";
              groupIds.add(idsCql);
            });
            return groupIds;
          })
    );
    groupIds.forEach(query -> {
      groupsBatch.add(usersLookUpService.lookUpGroupsUsingCQL(new OkapiParams(okapiHeaders), query));
    });
    groupsBatch.forEach(collectionCompletableFuture ->
      collectionCompletableFuture
        .thenApply(groupCollection -> {
          groups.addAll(groupCollection);
          return groups;
        }));
    usersBatch.forEach(collectionCompletableFuture ->
        collectionCompletableFuture
          .thenApply(userCollection -> {
            users.addAll(userCollection);
            return users;
          })
    );

    var assignedUserCollection = CompletableFuture.completedFuture(users).thenApply(userCollectionConverter::convert);

    assignedUserCollection.thenCombine(CompletableFuture.completedFuture(groups),
      (assignedUserCollection1, groups1) -> {
        groups1.forEach(group -> {
          assignedUserCollection1.getData().forEach(assignedUser -> {
            if (group.getId().equals(assignedUser.getAttributes().getPatronGroup()))
              assignedUser.getAttributes().setPatronGroup(group.getGroup());
          });
        });
        return null;
      });

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
