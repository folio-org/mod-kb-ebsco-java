package org.folio.service.kbcredentials;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.RequestHeadersUtil.tenantId;

import io.vertx.core.Context;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
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
import org.folio.rest.util.RequestHeadersUtil;
import org.folio.rest.validator.kbcredentials.KbCredentialsPatchBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPostBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPutBodyValidator;
import org.folio.service.exc.ServiceExceptions;
import org.folio.service.holdings.HoldingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

@Log4j2
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
    String tenantId = tenantId(okapiHeaders);
    log.debug("save:: by [id: {}, tenant: {}]", entity.getData().getId(), tenantId);

    postBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    return prepareAndSave(completedFuture(convertToDb(kbCredentials)), this::prepareSaveEntity, okapiHeaders)
      .thenCompose(
        credentials -> holdingsService.setUpCredentials(fromUUID(credentials.getId()), tenantId)
          .thenApply(unused -> convertToCredentials(credentials))
      );
  }

  @Override
  public CompletableFuture<Void> update(String id, KbCredentialsPutRequest entity, Map<String, String> okapiHeaders) {
    log.debug("update:: by [id: {}, tenant: {}]", id, tenantId(okapiHeaders));

    putBodyValidator.validate(entity);
    KbCredentials kbCredentials = entity.getData();
    KbCredentialsDataAttributes attributes = kbCredentials.getAttributes();
    return prepareAndSave(fetchDbKbCredentials(id, okapiHeaders),
      (dbCredentials, userId) -> prepareUpdateEntity(dbCredentials, attributes, userId), okapiHeaders)
      .thenApply(dbKbCredentials -> null);
  }

  @Override
  public CompletableFuture<Void> updatePartially(String id, KbCredentialsPatchRequest entity,
                                                 Map<String, String> okapiHeaders) {
    log.debug("updatePartially:: by [id: {}, tenant: {}]", id, tenantId(okapiHeaders));

    KbCredentials patchRequestData = convertPatchToCredentials(entity);
    patchBodyValidator.validate(patchRequestData);
    KbCredentialsDataAttributes attributes = patchRequestData.getAttributes();
    return prepareAndSave(fetchDbKbCredentials(id, okapiHeaders),
      (dbCredentials, userId) -> preparePartialUpdateEntity(dbCredentials, attributes, userId), okapiHeaders)
      .thenApply(dbKbCredentials -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String id, Map<String, String> okapiHeaders) {
    String tenantId = tenantId(okapiHeaders);
    log.info("delete:: Attempts to delete by [id: {}, tenant: {}]", id, tenantId);

    return repository.delete(toUUID(id), tenantId);
  }

  private DbKbCredentials preparePartialUpdateEntity(DbKbCredentials existingCredentials,
                                                     KbCredentialsDataAttributes attributes, String userId) {
    return setUpdateMeta(existingCredentials.toBuilder()
      .name(defaultIfBlank(attributes.getName(), existingCredentials.getName()))
      .url(defaultIfBlank(attributes.getUrl(), existingCredentials.getUrl()))
      .apiKey(defaultIfBlank(attributes.getApiKey(), existingCredentials.getApiKey()))
      .customerId(defaultIfBlank(attributes.getCustomerId(), existingCredentials.getCustomerId())), userId)
      .build();
  }

  private DbKbCredentials prepareUpdateEntity(DbKbCredentials existingCredentials,
                                              KbCredentialsDataAttributes attributes,
                                              String userId) {
    return setUpdateMeta(existingCredentials.toBuilder()
      .name(attributes.getName())
      .url(attributes.getUrl())
      .apiKey(attributes.getApiKey())
      .customerId(attributes.getCustomerId()), userId)
      .build();
  }

  private DbKbCredentials.DbKbCredentialsBuilder<?, ?> setUpdateMeta(
    DbKbCredentials.DbKbCredentialsBuilder<?, ?> builder,
    String userId) {
    return builder
      .updatedDate(OffsetDateTime.now())
      .updatedByUserId(toUUID(userId));
  }

  private CompletableFuture<DbKbCredentials> prepareAndSave(
    CompletableFuture<DbKbCredentials> credentialsFuture,
    BiFunction<DbKbCredentials, String, DbKbCredentials> prepareEntityFn,
    Map<String, String> okapiHeaders) {
    return credentialsFuture
      .thenCombine(RequestHeadersUtil.userIdFuture(okapiHeaders), prepareEntityFn)
      .thenCompose(dbKbCredentials -> verifyAndSave(dbKbCredentials, okapiHeaders));
  }

  private DbKbCredentials prepareSaveEntity(DbKbCredentials dbKbCredentials, String userId) {
    return dbKbCredentials.toBuilder()
      .createdDate(OffsetDateTime.now())
      .createdByUserId(toUUID(userId))
      .build();
  }

  private CompletableFuture<DbKbCredentials> verifyAndSave(DbKbCredentials dbKbCredentials,
                                                           Map<String, String> okapiHeaders) {

    String tenantId = tenantId(okapiHeaders);
    log.info("verifyAndSave:: Attempting to save by [id: {}, tenant: {}]", fromUUID(dbKbCredentials.getId()),
      tenantId);

    return verifyCredentials(dbKbCredentials, okapiHeaders)
      .thenCompose(v -> repository.save(dbKbCredentials, tenantId));
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
