package org.folio.service.users;

import java.util.concurrent.CompletableFuture;

import org.folio.common.OkapiParams;

public interface UsersService {

  CompletableFuture<User> findByToken(OkapiParams okapiParams);

  CompletableFuture<User> findById(String userId, OkapiParams okapiParams);

  CompletableFuture<User> save(User user, OkapiParams okapiParams);

  CompletableFuture<Void> update(User user, OkapiParams okapiParams);
}
