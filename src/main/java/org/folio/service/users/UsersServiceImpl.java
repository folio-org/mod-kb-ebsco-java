package org.folio.service.users;

import java.util.concurrent.CompletableFuture;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.repository.users.DbUser;
import org.folio.repository.users.UsersRepository;
import org.folio.service.exc.ServiceExceptions;
import org.folio.util.TokenUtils;

@Component
public class UsersServiceImpl implements UsersService {

  private final UsersRepository repository;
  private final UsersLookUpService lookUpService;
  private final Converter<DbUser, User> fromDbConverter;
  private final Converter<User, DbUser> toDbConverter;

  public UsersServiceImpl(UsersRepository repository, UsersLookUpService lookUpService,
                          Converter<DbUser, User> fromDbConverter,
                          Converter<User, DbUser> toDbConverter) {
    this.repository = repository;
    this.lookUpService = lookUpService;
    this.fromDbConverter = fromDbConverter;
    this.toDbConverter = toDbConverter;
  }

  @Override
  public CompletableFuture<User> findByToken(OkapiParams okapiParams) {
    return TokenUtils.fetchUserInfo(okapiParams.getHeaders())
      .thenCompose(userInfo -> findById(userInfo.getUserId(), okapiParams));
  }

  @Override
  public CompletableFuture<User> findById(String userId, OkapiParams okapiParams) {
    return repository.findById(userId, okapiParams.getTenant())
      .thenCompose(dbUser -> dbUser.map(user -> CompletableFuture.completedFuture(fromDbConverter.convert(user)))
        .orElseGet(() -> lookUpService.lookUpUser(okapiParams).thenCompose(user -> save(user, okapiParams))));
  }

  @Override
  public CompletableFuture<User> save(User user, OkapiParams okapiParams) {
    return repository.save(toDbConverter.convert(user), okapiParams.getTenant())
      .thenApply(fromDbConverter::convert);
  }

  @Override
  public CompletableFuture<Void> update(User user, OkapiParams okapiParams) {
    return repository.update(toDbConverter.convert(user), okapiParams.getTenant())
      .thenApply(updated -> {
        if (updated)
          return null;
        throw ServiceExceptions.notFound(User.class, user.getId());
      });
  }
}
