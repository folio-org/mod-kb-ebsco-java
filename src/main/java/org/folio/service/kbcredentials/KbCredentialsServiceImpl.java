package org.folio.service.kbcredentials;

import static java.util.Objects.requireNonNull;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.NotAuthorizedException;

import io.vertx.core.Context;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.util.TokenUtil;
import org.folio.rest.validator.KbCredentialsPostBodyValidator;

@Component
public class KbCredentialsServiceImpl implements KbCredentialsService {

  private static final String INVALID_TOKEN_MESSAGE = "Invalid token";
  private static final String CREDENTIALS_NAME_UNIQUENESS_MESSAGE = "Invalid name";
  private static final String CREDENTIALS_NAME_UNIQUENESS_DETAILS = "Credentials with name '%s' already exist";

  @Autowired
  private KbCredentialsRepository repository;

  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;
  @Autowired
  private Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter;
  @Autowired
  private Converter<KbCredentials, DbKbCredentials> credentialsToDBConverter;
  @Autowired
  private Converter<Collection<DbKbCredentials>, KbCredentialsCollection> collectionConverter;

  @Autowired
  private KbCredentialsPostBodyValidator postBodyValidator;

  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private Context context;

  @Override
  public CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders))
      .thenApply(collectionConverter::convert);
  }

  @Override
  public CompletableFuture<KbCredentials> save(KbCredentialsPostRequest entity, Map<String, String> okapiHeaders) {
    postBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    return verifyCredentials(kbCredentials, okapiHeaders)
      .thenApply(o -> fetchUserInfo(okapiHeaders))
      .thenApply(userInfo -> requireNonNull(credentialsToDBConverter.convert(kbCredentials))
        .toBuilder()
        .createdDate(Instant.now())
        .createdByUserId(userInfo.getKey())
        .createdByUserName(userInfo.getValue())
        .build())
      .thenCompose(dbKbCredentials -> repository.save(dbKbCredentials, tenantId(okapiHeaders)))
      .thenApply(credentialsFromDBConverter::convert);
  }

  private CompletableFuture<Void> verifyCredentials(KbCredentials kbCredentials, Map<String, String> okapiHeaders) {
    String tenantId = tenantId(okapiHeaders);
    return verifyNameUniqueness(kbCredentials.getAttributes().getName(), tenantId)
      .thenApply(aVoid -> configurationConverter.convert(kbCredentials))
      .thenCompose(configuration -> configurationService.verifyCredentials(configuration, context, tenantId))
      .thenCompose(errors -> {
        if (!errors.isEmpty()) {
          CompletableFuture<Void> future = new CompletableFuture<>();
          future.completeExceptionally(new ConfigurationInvalidException(errors));
          return future;
        }
        return CompletableFuture.completedFuture(null);
      });
  }

  private CompletableFuture<Void> verifyNameUniqueness(String name, String tenantId) {
    return repository.findAll(tenantId)
      .thenAccept(kbCredentials -> kbCredentials.stream()
        .filter(credentials -> credentials.getName().equals(name))
        .findFirst()
        .ifPresent(dbKbCredentials -> {
          throw new InputValidationException(
            CREDENTIALS_NAME_UNIQUENESS_MESSAGE,
            String.format(CREDENTIALS_NAME_UNIQUENESS_DETAILS, name));
        }));
  }

  private Pair<String, String> fetchUserInfo(Map<String, String> okapiHeaders) {
    Optional<Pair<String, String>> tokenInfo = TokenUtil.userFromToken(okapiHeaders.get(XOkapiHeaders.TOKEN));
    return tokenInfo.orElseThrow(() -> new NotAuthorizedException(INVALID_TOKEN_MESSAGE));
  }

}
