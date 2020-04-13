package org.folio.service.assignedusers;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;

@Component
public class AssignedUsersServiceImpl implements AssignedUsersService {

  @Autowired
  private AssignedUserRepository repository;
  @Autowired
  private Converter<Collection<DbAssignedUser>, AssignedUserCollection> collectionConverter;

  @Override
  public CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(credentialsId, tenantId(okapiHeaders)).thenApply(collectionConverter::convert);
  }
}
