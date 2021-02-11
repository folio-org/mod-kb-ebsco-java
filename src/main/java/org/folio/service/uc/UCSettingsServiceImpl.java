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

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.ConversionService;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.configuration.UCConfiguration;
import org.folio.client.uc.model.UCMetricType;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.repository.uc.DbUCSettings;
import org.folio.repository.uc.UCSettingsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsKey;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rmapi.result.UCSettingsResult;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.util.UserInfo;

@AllArgsConstructor
public class UCSettingsServiceImpl implements UCSettingsService {

  private static final String NOT_ENABLED_MESSAGE = "Usage Consolidation is not enabled for KB credentials with id [%s]";

  private final KbCredentialsService kbCredentialsService;
  private final UCSettingsRepository repository;
  private final UCAuthService authService;
  private final UCApigeeEbscoClient ebscoClient;
  private final ConversionService conversionService;

  @Override
  public CompletableFuture<UCSettings> fetchByUser(boolean includeMetricType, Map<String, String> okapiHeaders) {
    return kbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(kbCredentials -> fetchByCredentialsId(kbCredentials.getId(), includeMetricType, okapiHeaders));
  }

  @Override
  public CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, boolean includeMetricType,
                                                            Map<String, String> okapiHeaders) {
    return fetchUCSettings(credentialsId, includeMetricType, okapiHeaders)
      .thenApply(this::convertResult);
  }

  @Override
  public CompletableFuture<UCSettingsKey> fetchKeyByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return fetchDbUCSettings(credentialsId, okapiHeaders)
      .thenApply(this::convertToKey);
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

  @Override
  public CompletableFuture<UCSettings> save(String id, UCSettingsPostRequest request, Map<String, String> okapiHeaders) {
    updateRequest(request, id);
    return fetchUserInfo(okapiHeaders)
      .thenCombine(completedFuture(convertToDb(request)),
        (userInfo, dbUcSettings) -> prepareSave(dbUcSettings, userInfo))
      .thenCompose(dbUcSettings -> save(dbUcSettings, okapiHeaders))
      .thenApply(dbUCSettings -> new UCSettingsResult(dbUCSettings, null))
      .thenApply(this::convertResult);
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
    return getUCConfiguration(ucSettings, okapiHeaders)
      .thenCompose(ebscoClient::verifyCredentials)
      .thenAccept(aBoolean -> {
        if (Boolean.FALSE.equals(aBoolean)) {
          throw new InputValidationException("Invalid UC Credentials", null);
        }
      });
  }

  private CompletableFuture<UCConfiguration> getUCConfiguration(DbUCSettings ucSettings, Map<String, String> okapiHeaders) {
    return authService.authenticate(okapiHeaders)
      .thenApply(authToken -> createConfiguration(ucSettings, authToken));
  }

  private UCConfiguration createConfiguration(DbUCSettings ucSettings, String authToken) {
    return UCConfiguration.builder().customerKey(ucSettings.getCustomerKey()).accessToken(authToken).build();
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

  private void updateRequest(UCSettingsPostRequest request, String credentialsId) {
    var attributes = request.getData().getAttributes();
    attributes.setCredentialsId(credentialsId);
    attributes.setPlatformType(ObjectUtils.defaultIfNull(attributes.getPlatformType(), PlatformType.ALL));
    attributes.setStartMonth(ObjectUtils.defaultIfNull(attributes.getStartMonth(), Month.JAN));
  }

  private DbUCSettings prepareSave(DbUCSettings request, UserInfo userInfo) {
    return request.toBuilder()
      .createdDate(OffsetDateTime.now())
      .createdByUserId(toUUID(userInfo.getUserId()))
      .createdByUserName(userInfo.getUserName())
      .build();
  }

  private Function<Optional<DbUCSettings>, DbUCSettings> getUCSettingsOrFail(String credentialsId) {
    return dbUCSettings -> dbUCSettings
      .orElseThrow(() -> new NotFoundException(String.format(NOT_ENABLED_MESSAGE, credentialsId)));
  }

  private CompletableFuture<UCSettingsResult> fetchUCSettings(String credentialsId,
                                                              boolean includeMetricType,
                                                              Map<String, String> okapiHeaders) {
    return fetchDbUCSettings(credentialsId, okapiHeaders)
      .thenCompose(dbUCSettings ->
        fetchMetricTypeIfNeeded(includeMetricType, okapiHeaders, dbUCSettings)
          .thenApply(ucMetricType -> new UCSettingsResult(dbUCSettings, ucMetricType))
      );
  }

  private CompletableFuture<UCMetricType> fetchMetricTypeIfNeeded(boolean includeMetricType,
                                                                  Map<String, String> okapiHeaders,
                                                                  DbUCSettings dbUCSettings) {
    return includeMetricType
      ? getUCConfiguration(dbUCSettings, okapiHeaders).thenCompose(ebscoClient::getUsageMetricType)
      : CompletableFuture.completedFuture(null);
  }

  private UCSettingsKey convertToKey(DbUCSettings dbUCSettings) {
    return conversionService.convert(dbUCSettings, UCSettingsKey.class);
  }

  private DbUCSettings convertToDb(UCSettingsPostRequest request) {
    return conversionService.convert(request, DbUCSettings.class);
  }

  private UCSettings convertResult(UCSettingsResult ucSettingsResult) {
    return conversionService.convert(ucSettingsResult, UCSettings.class);
  }
}
