package org.folio.service.uc;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.TokenUtils.fetchUserInfo;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.UCConfiguration;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.repository.uc.DbUCSettings;
import org.folio.repository.uc.UCSettingsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rest.validator.uc.UsageConsolidationPostBodyValidator;
import org.folio.util.UserInfo;

@Service
public class UCSettingsServiceImpl implements UCSettingsService {

  private static final String NOT_ENABLED_MESSAGE = "Usage Consolidation is not enabled for KB credentials with id [%s]";

  private final UCSettingsRepository repository;
  private final UCAuthService authService;
  private final UCApigeeEbscoClient ebscoClient;
  private final Converter<DbUCSettings, UCSettings> fromDbConverter;
  private final Converter<UCSettingsPostRequest, DbUCSettings> toDbConverter;

  @Autowired
  private UsageConsolidationPostBodyValidator validator;

  public UCSettingsServiceImpl(UCSettingsRepository repository,
                               UCAuthService authService,
                               UCApigeeEbscoClient ebscoClient,
                               Converter<DbUCSettings, UCSettings> fromConverter,
                               Converter<UCSettingsPostRequest, DbUCSettings> toConverter) {
    this.repository = repository;
    this.authService = authService;
    this.ebscoClient = ebscoClient;
    this.fromDbConverter = fromConverter;
    this.toDbConverter = toConverter;
  }

  @Override
  public CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return fetchDbUCSettings(credentialsId, okapiHeaders).thenApply(fromDbConverter::convert);
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, UCSettingsPatchRequest patchRequest,
                                        Map<String, String> okapiHeaders) {
    return fetchDbUCSettings(credentialsId, okapiHeaders)
      .thenCombine(fetchUserInfo(okapiHeaders),
        (dbUcSettings, userInfo) -> prepareUpdate(dbUcSettings, patchRequest, userInfo))
      .thenCompose(dbUcSettings -> save(dbUcSettings, okapiHeaders))
      .thenApply(nothing());
  }

  private CompletableFuture<DbUCSettings> save(DbUCSettings ucSettings, Map<String, String> okapiHeaders) {
    CompletableFuture<DbUCSettings> result = new CompletableFuture<>();
    validate(ucSettings, okapiHeaders)
      .thenCompose(unused -> repository.save(ucSettings, tenantId(okapiHeaders)))
      .whenComplete(handleRepositoryException(result));
    return result;
  }

  private BiConsumer<DbUCSettings, Throwable> handleRepositoryException(CompletableFuture<DbUCSettings> result) {
    return (settings, throwable) -> {
      if (throwable == null) {
        result.complete(settings);
      } else {
        if (throwable.getCause() instanceof ConstraintViolationException) {
          ConstraintViolationException cause = (ConstraintViolationException) throwable.getCause();
          Map<String, String> invalidValues = cause.getInvalidValues();
          String detailedMessage = invalidValues.entrySet().stream()
            .map(entry -> String.format("Value '%s' is invalid for '%s'.", entry.getValue(), entry.getKey()))
            .collect(Collectors.joining(" "));
          InputValidationException exception = new InputValidationException("Invalid value", detailedMessage);
          result.completeExceptionally(exception);
        } else {
          result.completeExceptionally(throwable);
        }
      }
    };
  }

  private CompletableFuture<Void> validate(DbUCSettings ucSettings, Map<String, String> okapiHeaders) {
    return authService.authenticate(okapiHeaders)
      .thenApply(authToken -> new UCConfiguration(ucSettings.getCustomerKey(), authToken))
      .thenCompose(ebscoClient::verifyCredentials)
      .thenAccept(aBoolean -> {
        if (Boolean.FALSE.equals(aBoolean)) {
          throw new InputValidationException("Invalid UC Credentials", null);
        }
      });
  }

  private DbUCSettings prepareUpdate(DbUCSettings dbUcSettings, UCSettingsPatchRequest patchRequest, UserInfo userInfo) {
    var patchAttributes = patchRequest.getData().getAttributes();
    return dbUcSettings.toBuilder()
      .customerKey(ObjectUtils.defaultIfNull(patchAttributes.getCustomerKey(), dbUcSettings.getCustomerKey()))
      .currency(ObjectUtils.defaultIfNull(patchAttributes.getCurrency(), dbUcSettings.getCurrency()).toUpperCase())
      .startMonth(patchAttributes.getStartMonth() == null
        ? dbUcSettings.getStartMonth()
        : patchAttributes.getStartMonth().value())
      .platformType(patchAttributes.getPlatformType() == null
        ? dbUcSettings.getPlatformType()
        : patchAttributes.getPlatformType().value())
      .updatedDate(OffsetDateTime.now())
      .updatedByUserId(toUUID(userInfo.getUserId()))
      .updatedByUserName(userInfo.getUserName())
      .build();
  }

  private CompletableFuture<DbUCSettings> fetchDbUCSettings(String credentialsId, Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(getUCSettingsOrFail(credentialsId));
  }

  @Override
  public CompletableFuture<UCSettings> save(String id, UCSettingsPostRequest request, Map<String, String> okapiHeaders) {
    validator.validate(request);
    request.getData().getAttributes().setCredentialsId(id);
    return completedFuture(toDbConverter.convert(request))
      .thenCombine(fetchUserInfo(okapiHeaders), this::setCreatorMetaInfo)
      .thenCompose(dbUcSettings -> save(dbUcSettings, okapiHeaders))
      .thenApply(fromDbConverter::convert);
  }

  private DbUCSettings setCreatorMetaInfo(DbUCSettings settings, UserInfo userInfo) {
      return settings.toBuilder()
        .createdDate(OffsetDateTime.now())
        .createdByUserId(toUUID(userInfo.getUserId()))
        .createdByUserName(userInfo.getUserName())
        .build();
  }

  private Function<Optional<DbUCSettings>, DbUCSettings> getUCSettingsOrFail(String credentialsId) {
    return dbUCSettings -> dbUCSettings
      .orElseThrow(() -> new NotFoundException(String.format(NOT_ENABLED_MESSAGE, credentialsId)));
  }
}
