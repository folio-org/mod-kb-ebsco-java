package org.folio.service.assignedusers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;

public interface AssignedUsersService {

  CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<AssignedUser> save(AssignedUserPostRequest entity, Map<String, String> okapiHeaders);
}
