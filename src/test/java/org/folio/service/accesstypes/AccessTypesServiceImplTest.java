package org.folio.service.accesstypes;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.common.OkapiParams;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.service.users.User;
import org.folio.service.users.UsersLookUpService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;

@RunWith(MockitoJUnitRunner.class)
public class AccessTypesServiceImplTest {

  private static final String TENANT_ID = "test";
  private static final Map<String, String> HEADERS = new CaseInsensitiveMap<>(Map.of(
    XOkapiHeaders.TENANT, TENANT_ID,
    XOkapiHeaders.URL, "http://test.url"
  ));

  @InjectMocks
  private AccessTypesServiceImpl accessTypesService;

  @Mock
  private UsersLookUpService usersLookUpService;
  @Mock
  private AccessTypesRepository repository;
  @Mock
  private Converter<List<AccessType>, AccessTypeCollection> accessTypeCollectionConverter;
  @Mock
  private Converter<DbAccessType, AccessType> accessTypeFromDbConverter;

  @Test
  public void findByCredentialsIdShouldPopulateUpdaterFromUpdatedByUserId() {
    var credentialsId = UUID.randomUUID();
    var creatorId = UUID.randomUUID();
    var updaterId = UUID.randomUUID();

    // Access type created by one user and last updated by a different user.
    var accessType = new AccessType().withMetadata(new Metadata()
      .withCreatedByUserId(creatorId.toString())
      .withUpdatedByUserId(updaterId.toString()));
    var dbAccessType = DbAccessType.builder().id(UUID.randomUUID()).credentialsId(credentialsId).build();
    when(repository.findByCredentialsId(credentialsId, TENANT_ID))
      .thenReturn(completedFuture(List.of(dbAccessType)));
    when(accessTypeFromDbConverter.convert(dbAccessType)).thenReturn(accessType);
    when(usersLookUpService.lookUpUsers(anyList(), any(OkapiParams.class)))
      .thenReturn(completedFuture(List.of(user(creatorId, "creator"), user(updaterId, "updater"))));
    when(accessTypeCollectionConverter.convert(anyList())).thenReturn(new AccessTypeCollection());

    getResult(accessTypesService.findByCredentialsId(credentialsId.toString(), HEADERS));

    // The updater's id must be part of the users lookup - regression guard for the
    // updatedByUsername/updatedByUserId mix-up that left updater unresolved.
    var idsCaptor = ArgumentCaptor.forClass(List.class);
    verify(usersLookUpService).lookUpUsers(idsCaptor.capture(), any(OkapiParams.class));
    assertThat(idsCaptor.getValue()).contains(creatorId, updaterId);
    // Enrichment mutates the access types in place before they are converted to the collection.
    assertThat(accessType.getUpdater()).isNotNull()
      .extracting("firstName", "lastName").containsExactly("updater_firstname", "updater_lastname");
    assertThat(accessType.getMetadata().getUpdatedByUsername()).isEqualTo("updater");
    assertThat(accessType.getCreator()).isNotNull()
      .extracting("firstName").isEqualTo("creator_firstname");
  }

  private User user(UUID id, String prefix) {
    return User.builder()
      .id(id.toString())
      .userName(prefix)
      .firstName(prefix + "_firstname")
      .lastName(prefix + "_lastname")
      .build();
  }

  @SneakyThrows
  private <T> void getResult(CompletableFuture<T> future) {
    future.get();
  }
}
