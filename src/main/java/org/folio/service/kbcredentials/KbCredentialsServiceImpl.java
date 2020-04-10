package org.folio.service.kbcredentials;

import static java.util.Objects.requireNonNull;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.NotAuthorizedException;

import io.vertx.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.util.TokenUtil;
import org.folio.rest.util.UserInfo;
import org.folio.rest.validator.kbcredentials.KbCredentialsPostBodyValidator;
import org.folio.rest.validator.kbcredentials.KbCredentialsPutBodyValidator;
import org.folio.service.exc.ServiceExceptions;

@Component
public class KbCredentialsServiceImpl implements KbCredentialsService {

  private static final String INVALID_TOKEN_MESSAGE = "Invalid token";

  @Autowired
  private KbCredentialsRepository repository;

  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;
  @Autowired
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
  private CustomLabelsService customLabelsService;
  @Autowired
  private Context context;

  @Override
  public CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders)).thenApply(credentialsCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<KbCredentials> findById(String id, Map<String, String> okapiHeaders) {
    return fetchDbKbCredentials(id, okapiHeaders).thenApply(credentialsFromDBConverter::convert);
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
        .createdByUserId(userInfo.getUserId())
        .createdByUserName(userInfo.getUsername())
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
      .thenApply(o -> fetchUserInfo(okapiHeaders))
      .thenCombine(fetchDbKbCredentials(id, okapiHeaders), (userInfo, dbKbCredentials) -> dbKbCredentials.toBuilder()
        .name(attributes.getName())
        .url(attributes.getUrl())
        .apiKey(attributes.getApiKey())
        .customerId(attributes.getCustomerId())
        .updatedDate(Instant.now())
        .updatedByUserId(userInfo.getUserId())
        .updatedByUserName(userInfo.getUsername())
        .build())
      .thenCompose(dbKbCredentials -> repository.save(dbKbCredentials, tenantId(okapiHeaders)))
      .thenApply(dbKbCredentials -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String id, Map<String, String> okapiHeaders) {
    return repository.delete(id, tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<CustomLabelsCollection> fetchCustomLabels(String id, Map<String, String> okapiHeaders) {
    return fetchDbKbCredentials(id, okapiHeaders)
      .thenApply(dbKbCredentials -> {
        KbCredentials kbCredentials = requireNonNull(credentialsFromDBConverter.convert(dbKbCredentials));
        kbCredentials.getAttributes().withApiKey(dbKbCredentials.getApiKey());
        return configurationConverter.convert(kbCredentials);
      })
      .thenCompose(configuration -> customLabelsService.fetchCustomLabels(configuration, okapiHeaders))
      .thenApply(customLabelsCollection -> {
        customLabelsCollection.getData().forEach(customLabel -> customLabel.setCredentialsId(id));
        return customLabelsCollection;
      });
  }

  private CompletableFuture<Void> verifyCredentials(KbCredentials kbCredentials, Map<String, String> okapiHeaders) {
    Configuration configuration = configurationConverter.convert(kbCredentials);
    return configurationService.verifyCredentials(configuration, context, tenantId(okapiHeaders))
      .thenCompose(errors -> {
        if (!errors.isEmpty()) {
          CompletableFuture<Void> future = new CompletableFuture<>();
          future.completeExceptionally(new ConfigurationInvalidException(errors));
          return future;
        }
        return CompletableFuture.completedFuture(null);
      });
  }

  private UserInfo fetchUserInfo(Map<String, String> okapiHeaders) {
    Optional<UserInfo> tokenInfo = TokenUtil.userInfoFromToken(okapiHeaders.get(XOkapiHeaders.TOKEN));
    return tokenInfo.orElseThrow(() -> new NotAuthorizedException(INVALID_TOKEN_MESSAGE));
  }

  private CompletableFuture<DbKbCredentials> fetchDbKbCredentials(String id, Map<String, String> okapiHeaders) {
    return repository.findById(id, tenantId(okapiHeaders)).thenApply(getCredentialsOrFail(id));
  }

  private Function<Optional<DbKbCredentials>, DbKbCredentials> getCredentialsOrFail(String id) {
    return credentials -> credentials.orElseThrow(() -> ServiceExceptions.notFound(KbCredentials.class, id));
  }
}
