package org.folio.service.assignedusers;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.NotFoundException;
import lombok.SneakyThrows;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.common.OkapiParams;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.converter.assignedusers.UserCollectionDataConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.service.users.Group;
import org.folio.service.users.User;
import org.folio.service.users.UsersLookUpService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignedUsersServiceImplTest {

  private static final String TENANT_ID = "test";
  private static final Map<String, String> HEADERS = new CaseInsensitiveMap<>(Map.of(
    XOkapiHeaders.TENANT, TENANT_ID,
    XOkapiHeaders.URL, "http://test.url"
  ));

  @InjectMocks
  private AssignedUsersServiceImpl usersService;

  @Mock
  private AssignedUserRepository assignedUserRepository;
  @Mock
  private UserCollectionDataConverter userCollectionConverter;
  @Mock
  private UsersLookUpService usersLookUpService;

  @Test
  public void testFindByCredentialsId_sortedByLastName() {
    var credId = UUID.randomUUID();
    var user1Id = UUID.randomUUID();
    var user2Id = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    when(assignedUserRepository.findByCredentialsId(credId, TENANT_ID))
      .thenReturn(completedFuture(List.of(
        DbAssignedUser.builder().credentialsId(credId).id(user1Id).build(),
        DbAssignedUser.builder().credentialsId(credId).id(user2Id).build()
      )));
    when(usersLookUpService.lookUpUsers(eq(List.of(user1Id, user2Id)), any(OkapiParams.class)))
      .thenReturn(completedFuture(List.of(
        User.builder().id(user1Id.toString())
          .patronGroup(groupId.toString())
          .firstName("b")
          .lastName("b")
          .userName("b")
          .build(),
        User.builder().id(user1Id.toString())
          .patronGroup(groupId.toString())
          .firstName("a")
          .lastName("a")
          .userName("a")
          .build()
      )));
    when(usersLookUpService.lookUpGroups(eq(List.of(groupId)), any(OkapiParams.class)))
      .thenReturn(completedFuture(List.of(
        Group.builder().id(groupId.toString()).group("group").build()
      )));

    getResult(usersService.findByCredentialsId(credId.toString(), HEADERS));

    var resultArgumentCaptor = ArgumentCaptor.forClass(UserCollectionDataConverter.UsersResult.class);
    verify(userCollectionConverter).convert(resultArgumentCaptor.capture());

    var captorValue = resultArgumentCaptor.getValue();
    assertThat(captorValue).isNotNull();
    assertThat(captorValue.groups())
      .hasSize(1)
      .extracting(Group::getGroup)
      .containsExactly("group");
    assertThat(captorValue.users())
      .hasSize(2)
      .extracting(User::getLastName)
      .containsExactly("a", "b");
  }

  @Test
  @SneakyThrows
  public void shouldNotAssignNonExistentUser() {
    var assignUserData = new AssignedUserId()
      .withId(UUID.randomUUID().toString())
      .withCredentialsId(UUID.randomUUID().toString());

    when(usersLookUpService.lookUpUserById(any(), any()))
      .thenReturn(CompletableFuture.failedFuture(new NotFoundException("User Not Found")));

    var assignedUser = usersService.save(assignUserData, HEADERS);

    assertThatThrownBy(assignedUser::get)
      .hasCauseInstanceOf(InputValidationException.class)
      .hasMessageEndingWith("Unable to assign user");
  }

  @Test
  @SneakyThrows
  public void shouldNotAssignUserIfLookupFails() {
    var assignUserData = new AssignedUserId()
      .withId(UUID.randomUUID().toString())
      .withCredentialsId(UUID.randomUUID().toString());
    var lookupExceptionMessage = "Some exception";

    when(usersLookUpService.lookUpUserById(any(), any()))
      .thenReturn(CompletableFuture.failedFuture(new Exception(lookupExceptionMessage)));

    var assignedUser = usersService.save(assignUserData, HEADERS);

    assertThatThrownBy(assignedUser::get)
      .hasCauseInstanceOf(IllegalStateException.class)
      .hasMessageEndingWith("Unable to lookup user: " + lookupExceptionMessage);
  }

  @SneakyThrows
  private <T> T getResult(CompletableFuture<T> future) {
    return future.get();
  }
}
