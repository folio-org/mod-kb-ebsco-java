package org.folio.service.kbcredentials;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.TokenUtils.fetchUserInfo;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;

import io.vertx.core.Context;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.validator.kbcredentials.KbCredentialsPostBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPutBodyValidator;
import org.folio.service.exc.ServiceExceptions;
import org.folio.util.UserInfo;

public class KbCredentialsServiceImpl implements KbCredentialsService {

  private static final String USER_CREDS_NOT_FOUND_MESSAGE = "User credentials not found: userId = %s";

  @Autowired
  private KbCredentialsRepository repository;

  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;
  private Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter;
  @Autowired
  private Converter<KbCredentials, DbKbCredentials> credentialsToDBConverter;
  @Autowired
  private Converter<Collection<DbKbCredentials>, KbCredentialsCollection> credentialsCollectionConverter;

  @Autowired
  private KbCredentialsPostBodyValidator postBodyValidator;
  @Autowired
  private KbCredentialsPutBodyValidator putBodyValidator;

  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private Context context;

  public KbCredentialsServiceImpl(
    Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter) {
    this.credentialsFromDBConverter = credentialsFromDBConverter;
  }

  private static <T> Function<Optional<T>, CompletableFuture<Optional<T>>> ifEmpty(
    Supplier<CompletableFuture<Optional<T>>> supplier) {
    return optional -> optional
      .map(value -> completedFuture(Optional.of(value)))
      .orElse(supplier.get());
  }

  @Override
  public CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders) {
    return fetchUserInfo(okapiHeaders)
      .thenCompose(userInfo -> findUserCredentials(userInfo, tenantId(okapiHeaders)))
      .thenApply(credentialsFromDBConverter::convert);
  }

  @Override
  public CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders))
      .thenApply(credentialsCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<KbCredentials> findById(String id, Map<String, String> okapiHeaders) {
    return fetchDbKbCredentials(id, okapiHeaders)
      .thenApply(credentialsFromDBConverter::convert);
  }

  @Override
  public CompletableFuture<KbCredentials> save(KbCredentialsPostRequest entity, Map<String, String> okapiHeaders) {
    postBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    return verifyCredentials(kbCredentials, okapiHeaders)
      .thenCompose(o -> fetchUserInfo(okapiHeaders))
      .thenApply(userInfo -> requireNonNull(credentialsToDBConverter.convert(kbCredentials))
        .toBuilder()
        .createdDate(Instant.now())
        .createdByUserId(userInfo.getUserId())
        .createdByUserName(userInfo.getUserName())
        .build())
      .thenCompose(dbKbCredentials -> repository.save(dbKbCredentials, tenantId(okapiHeaders)))
      .thenApply(credentialsFromDBConverter::convert);
  }

  @Override
  public CompletableFuture<Void> update(String id, KbCredentialsPutRequest entity, Map<String, String> okapiHeaders) {
    putBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    KbCredentialsDataAttributes attributes = kbCredentials.getAttributes();
    return verifyCredentials(kbCredentials, okapiHeaders)
      .thenCompose(o -> fetchUserInfo(okapiHeaders))
      .thenCombine(fetchDbKbCredentials(id, okapiHeaders), (userInfo, dbKbCredentials) -> dbKbCredentials.toBuilder()
        .name(attributes.getName())
        .url(attributes.getUrl())
        .apiKey(attributes.getApiKey())
        .customerId(attributes.getCustomerId())
        .updatedDate(Instant.now())
        .updatedByUserId(userInfo.getUserId())
        .updatedByUserName(userInfo.getUserName())
        .build())
      .thenCompose(dbKbCredentials -> repository.save(dbKbCredentials, tenantId(okapiHeaders)))
      .thenApply(dbKbCredentials -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String id, Map<String, String> okapiHeaders) {
    return repository.delete(id, tenantId(okapiHeaders));
  }

  private CompletableFuture<Void> verifyCredentials(KbCredentials kbCredentials, Map<String, String> okapiHeaders) {
    Configuration configuration = configurationConverter.convert(kbCredentials);
    return configurationService.verifyCredentials(configuration, context, new OkapiData(okapiHeaders))
      .thenCompose(errors -> {
        if (!errors.isEmpty()) {
          CompletableFuture<Void> future = new CompletableFuture<>();
          future.completeExceptionally(new ConfigurationInvalidException(errors));
          return future;
        }
        return completedFuture(null);
      });
  }

  private CompletableFuture<DbKbCredentials> fetchDbKbCredentials(String id, Map<String, String> okapiHeaders) {
    return repository.findById(id, tenantId(okapiHeaders)).thenApply(getCredentialsOrFail(id));
  }

  private CompletionStage<DbKbCredentials> findUserCredentials(UserInfo userInfo, String tenant) {
    return repository.findByUserId(userInfo.getUserId(), tenant)
      .thenCompose(ifEmpty(() -> findSingleKbCredentials(tenant)))
      .thenApply(getCredentialsOrFailWithUserId(userInfo.getUserId()));
  }

  private CompletableFuture<Optional<DbKbCredentials>> findSingleKbCredentials(String tenant) {
    CompletableFuture<Collection<DbKbCredentials>> allCreds = repository.findAll(tenant);

    return allCreds.thenApply(credentials ->
      credentials.size() == 1
        ? Optional.of(CollectionUtils.extractSingleton(credentials))
        : Optional.empty()
    );
  }

  private Function<Optional<DbKbCredentials>, DbKbCredentials> getCredentialsOrFail(String id) {
    return credentials -> credentials.orElseThrow(() -> ServiceExceptions.notFound(KbCredentials.class, id));
  }

  private Function<Optional<DbKbCredentials>, DbKbCredentials> getCredentialsOrFailWithUserId(String userId) {
    return credentials -> credentials.orElseThrow(
      () -> new NotFoundException(format(USER_CREDS_NOT_FOUND_MESSAGE, userId)));
  }

}
