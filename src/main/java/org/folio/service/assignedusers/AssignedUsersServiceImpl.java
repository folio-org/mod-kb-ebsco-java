package org.folio.service.assignedusers;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.service.exc.ServiceExceptions;

@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  @Autowired
  private AssignedUserRepository repository;
  @Autowired
  private Converter<Collection<DbAssignedUser>, AssignedUserCollection> collectionConverter;
  @Autowired
  private Converter<DbAssignedUser, AssignedUser> fromDbConverter;

  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId,
                                                                       Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(credentialsId, tenantId(okapiHeaders)).thenApply(collectionConverter::convert);
  }

  @Override
  public CompletableFuture<AssignedUser> findByCredentialsIdAndUserId(String credentialsId, String userId,
                                                                      Map<String, String> okapiHeaders) {
    return repository.findByCredentialsIdAndUserId(credentialsId, userId, tenantId(okapiHeaders))
      .thenApply(getAssignedUserOrFail(userId))
      .thenApply(fromDbConverter::convert);
  }

  private Function<Optional<DbAssignedUser>, DbAssignedUser> getAssignedUserOrFail(String id) {
    return user -> user.orElseThrow(() -> ServiceExceptions.notFound(AssignedUser.class, id));
  }
}
