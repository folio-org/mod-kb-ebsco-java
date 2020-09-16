package org.folio.service.kbcredentials;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.TokenUtils.fetchUserInfo;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.vertx.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.repository.holdings.status.retry.RetryStatusRepository;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.validator.kbcredentials.KbCredentialsPatchBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPostBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPutBodyValidator;
import org.folio.service.exc.ServiceExceptions;
import org.folio.util.UserInfo;

public class KbCredentialsServiceImpl implements KbCredentialsService {

  @Autowired
  private KbCredentialsRepository repository;
  @Autowired
  private HoldingsStatusRepository holdingsStatusRepository;
  @Autowired
  private RetryStatusRepository retryStatusRepository;

  @Autowired
  private Converter<DbKbCredentials, Configuration> configurationConverter;
  private final Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter;
  @Autowired
  private Converter<KbCredentials, DbKbCredentials> credentialsToDBConverter;
  private final Converter<Collection<DbKbCredentials>, KbCredentialsCollection> credentialsCollectionConverter;
  @Autowired
  private Converter<KbCredentialsPatchRequest, KbCredentials> pathRequestConverter;

  @Autowired
  private KbCredentialsPostBodyValidator postBodyValidator;
  @Autowired
  private KbCredentialsPutBodyValidator putBodyValidator;
  @Autowired
  private KbCredentialsPatchBodyValidator patchBodyValidator;

  @Autowired
  private ConfigurationService configurationService;
  private final UserKbCredentialsService userKbCredentialsService;
  @Autowired
  private Context context;

  public KbCredentialsServiceImpl(Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter,
                                  UserKbCredentialsService userKbCredentialsService,
                                  Converter<Collection<DbKbCredentials>, KbCredentialsCollection> credentialsCollectionConverter) {
    this.credentialsFromDBConverter = credentialsFromDBConverter;
    this.userKbCredentialsService = userKbCredentialsService;
    this.credentialsCollectionConverter = credentialsCollectionConverter;
  }

  @Override
  public CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders) {
    return userKbCredentialsService.findByUser(okapiHeaders);
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
    return prepareAndSave(completedFuture(requireNonNull(credentialsToDBConverter.convert(kbCredentials))),
      this::prepareSaveEntity, okapiHeaders)
      .thenApply(credentialsFromDBConverter::convert)
      .thenApply(credentials -> insertLoadingStatusNotStarted(credentials, tenantId(okapiHeaders)));
  }

  @Override
  public CompletableFuture<Void> update(String id, KbCredentialsPutRequest entity, Map<String, String> okapiHeaders) {
    putBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    KbCredentialsDataAttributes attributes = kbCredentials.getAttributes();
    return prepareAndSave(fetchDbKbCredentials(id, okapiHeaders),
      (dbCredentials, userInfo) -> prepareUpdateEntity(dbCredentials, attributes, userInfo), okapiHeaders)
      .thenApply(dbKbCredentials -> null);
  }

  @Override
  public CompletableFuture<Void> updatePartially(String id, KbCredentialsPatchRequest entity,
                                                 Map<String, String> okapiHeaders) {
    KbCredentials patchRequestData = requireNonNull(pathRequestConverter.convert(entity));
    patchBodyValidator.validate(patchRequestData);
    KbCredentialsDataAttributes attributes = patchRequestData.getAttributes();
    return prepareAndSave(fetchDbKbCredentials(id, okapiHeaders),
      (dbCredentials, userInfo) -> preparePartialUpdateEntity(dbCredentials, attributes, userInfo), okapiHeaders)
      .thenApply(dbKbCredentials -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String id, Map<String, String> okapiHeaders) {
    return repository.delete(toUUID(id), tenantId(okapiHeaders));
  }

  private DbKbCredentials preparePartialUpdateEntity(DbKbCredentials existingCredentials,
                                                     KbCredentialsDataAttributes attributes, UserInfo userInfo) {
    return setUpdateMeta(existingCredentials.toBuilder()
      .name(defaultIfBlank(attributes.getName(), existingCredentials.getName()))
      .url(defaultIfBlank(attributes.getUrl(), existingCredentials.getUrl()))
      .apiKey(defaultIfBlank(attributes.getApiKey(), existingCredentials.getApiKey()))
      .customerId(defaultIfBlank(attributes.getCustomerId(), existingCredentials.getCustomerId())), userInfo)
      .build();
  }

  private DbKbCredentials prepareUpdateEntity(DbKbCredentials existingCredentials, KbCredentialsDataAttributes attributes,
                                              UserInfo userInfo) {
    return setUpdateMeta(existingCredentials.toBuilder()
      .name(attributes.getName())
      .url(attributes.getUrl())
      .apiKey(attributes.getApiKey())
      .customerId(attributes.getCustomerId()), userInfo)
      .build();
  }

  private DbKbCredentials.DbKbCredentialsBuilder<?, ?> setUpdateMeta(DbKbCredentials.DbKbCredentialsBuilder<?, ?> builder,
                                                                     UserInfo userInfo) {
    return builder
      .updatedDate(OffsetDateTime.now())
      .updatedByUserId(toUUID(userInfo.getUserId()))
      .updatedByUserName(userInfo.getUserName());
  }

  private CompletableFuture<DbKbCredentials> prepareAndSave(CompletableFuture<DbKbCredentials> credentialsFuture,
                                                            BiFunction<DbKbCredentials, UserInfo, DbKbCredentials> prepareEntityFn,
                                                            Map<String, String> okapiHeaders) {
    return credentialsFuture
      .thenCombine(fetchUserInfo(okapiHeaders), prepareEntityFn)
      .thenCompose(dbKbCredentials -> verifyAndSave(dbKbCredentials, okapiHeaders));
  }

  private DbKbCredentials prepareSaveEntity(DbKbCredentials dbKbCredentials, UserInfo userInfo) {
    return dbKbCredentials.toBuilder()
      .createdDate(OffsetDateTime.now())
      .createdByUserId(toUUID(userInfo.getUserId()))
      .createdByUserName(userInfo.getUserName())
      .build();
  }

  private KbCredentials insertLoadingStatusNotStarted(KbCredentials credentials, String tenantId) {
    final UUID credentialsId = toUUID(credentials.getId());
    holdingsStatusRepository.save(getStatusNotStarted(), credentialsId, tenantId)
      .thenCompose(v -> resetRetryStatus(credentialsId, tenantId));
    return credentials;
  }

  private CompletionStage<Void> resetRetryStatus(UUID credentialsId, String tenantId) {
    return retryStatusRepository.delete(credentialsId, tenantId)
      .thenCompose(o -> retryStatusRepository.save(new RetryStatus(0, null), credentialsId, tenantId));
  }

  private CompletableFuture<DbKbCredentials> verifyAndSave(DbKbCredentials dbKbCredentials,
                                                           Map<String, String> okapiHeaders) {
    return verifyCredentials(dbKbCredentials, okapiHeaders)
      .thenCompose(v -> repository.save(dbKbCredentials, tenantId(okapiHeaders)));
  }

  private CompletableFuture<Void> verifyCredentials(DbKbCredentials dbKbCredentials, Map<String, String> okapiHeaders) {
    Configuration configuration = configurationConverter.convert(dbKbCredentials);
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
    return repository.findById(toUUID(id), tenantId(okapiHeaders)).thenApply(getCredentialsOrFail(id));
  }

  private Function<Optional<DbKbCredentials>, DbKbCredentials> getCredentialsOrFail(String id) {
    return credentials -> credentials.orElseThrow(() -> ServiceExceptions.notFound(KbCredentials.class, id));
  }

}
