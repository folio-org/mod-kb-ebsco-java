package org.folio.service.assignedusers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;

public interface AssignedUsersService {

  CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<AssignedUserId> save(AssignedUserId assignedUserId, Map<String, String> okapiHeaders);

  CompletableFuture<Void> delete(String credentialsId, String userId, Map<String, String> okapiHeaders);

}
