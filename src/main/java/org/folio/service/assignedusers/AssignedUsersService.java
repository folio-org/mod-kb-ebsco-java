package org.folio.service.assignedusers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;

public interface AssignedUsersService {

  CompletableFuture<AssignedUserCollection> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<AssignedUser> findByCredentialsIdAndUserId(String credentialsId, String userId,
                                                               Map<String, String> okapiHeaders);
}