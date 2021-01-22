package org.folio.service.kbcredentials;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.TokenUtils.fetchUserInfo;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.vertx.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsKey;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.validator.kbcredentials.KbCredentialsPatchBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPostBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPutBodyValidator;
import org.folio.service.exc.ServiceExceptions;
import org.folio.service.holdings.HoldingsService;
import org.folio.util.UserInfo;

public class KbCredentialsServiceImpl implements KbCredentialsService {

  private final UserKbCredentialsService userKbCredentialsService;
  private final ConversionService conversionService;
  @Autowired
  private KbCredentialsRepository repository;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private KbCredentialsPostBodyValidator postBodyValidator;
  @Autowired
  private KbCredentialsPutBodyValidator putBodyValidator;
  @Autowired
  private KbCredentialsPatchBodyValidator patchBodyValidator;
  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private Context context;


  public KbCredentialsServiceImpl(UserKbCredentialsService userKbCredentialsService,
                                  ConversionService conversionService) {
    this.conversionService = conversionService;
    this.userKbCredentialsService = userKbCredentialsService;
  }

  @Override
  public CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders) {
    return userKbCredentialsService.findByUser(okapiHeaders);
  }

  @Override
  public CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders))
      .thenApply(this::convertToCollection);
  }

  @Override
  public CompletableFuture<KbCredentials> findById(String id, Map<String, String> okapiHeaders) {
    return fetchDbKbCredentials(id, okapiHeaders)
      .thenApply(this::convertToCredentials);
  }

  @Override
  public CompletableFuture<KbCredentialsKey> findKeyById(String id, Map<String, String> okapiHeaders) {
    return fetchDbKbCredentials(id, okapiHeaders)
      .thenApply(this::convertToCredentialsKey);
  }

  @Override
  public CompletableFuture<KbCredentials> save(KbCredentialsPostRequest entity, Map<String, String> okapiHeaders) {
    postBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    return prepareAndSave(completedFuture(convertToDb(kbCredentials)), this::prepareSaveEntity, okapiHeaders)
      .thenCompose(credentials -> holdingsService.setUpCredentials(fromUUID(credentials.getId()), tenantId(okapiHeaders))
        .thenApply(unused -> convertToCredentials(credentials))
      );
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
    KbCredentials patchRequestData = convertPatchToCredentials(entity);
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

  private CompletableFuture<DbKbCredentials> verifyAndSave(DbKbCredentials dbKbCredentials,
                                                           Map<String, String> okapiHeaders) {
    return verifyCredentials(dbKbCredentials, okapiHeaders)
      .thenCompose(v -> repository.save(dbKbCredentials, tenantId(okapiHeaders)));
  }

  private CompletableFuture<Void> verifyCredentials(DbKbCredentials dbKbCredentials, Map<String, String> okapiHeaders) {
    Configuration configuration = convertToConfiguration(dbKbCredentials);
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


  private KbCredentialsCollection convertToCollection(Collection<DbKbCredentials> dbKbCredentials) {
    return requireNonNull(conversionService.convert(dbKbCredentials, KbCredentialsCollection.class));
  }

  private KbCredentials convertToCredentials(DbKbCredentials dbKbCredentials) {
    return requireNonNull(conversionService.convert(dbKbCredentials, KbCredentials.class));
  }

  private KbCredentialsKey convertToCredentialsKey(DbKbCredentials dbKbCredentials) {
    return requireNonNull(conversionService.convert(dbKbCredentials, KbCredentialsKey.class));
  }

  private DbKbCredentials convertToDb(KbCredentials kbCredentials) {
    return requireNonNull(conversionService.convert(kbCredentials, DbKbCredentials.class));
  }

  private KbCredentials convertPatchToCredentials(KbCredentialsPatchRequest entity) {
    return requireNonNull(conversionService.convert(entity, KbCredentials.class));
  }

  private Configuration convertToConfiguration(DbKbCredentials dbKbCredentials) {
    return requireNonNull(conversionService.convert(dbKbCredentials, Configuration.class));
  }
}
