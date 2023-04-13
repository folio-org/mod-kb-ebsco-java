package org.folio.service.uc;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.common.FunctionUtils.nothing;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.TenantUtil.tenantId;
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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.client.uc.UcApigeeEbscoClient;
import org.folio.client.uc.configuration.UcConfiguration;
import org.folio.client.uc.model.UcMetricType;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.repository.uc.DbUcSettings;
import org.folio.repository.uc.UcSettingsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsKey;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rmapi.result.UcSettingsResult;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.util.UserInfo;
import org.springframework.core.convert.ConversionService;

@Log4j2
@AllArgsConstructor
public class UcSettingsServiceImpl implements UcSettingsService {

  private static final String NOT_ENABLED_MESSAGE =
    "Usage Consolidation is not enabled for KB credentials with id [%s]";

  private final KbCredentialsService kbCredentialsService;
  private final UcSettingsRepository repository;
  private final UcAuthService authService;
  private final UcApigeeEbscoClient ebscoClient;
  private final ConversionService conversionService;

  @Override
  public CompletableFuture<UCSettings> fetchByUser(boolean includeMetricType, Map<String, String> okapiHeaders) {
    return kbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(kbCredentials -> fetchByCredentialsId(kbCredentials.getId(), includeMetricType, okapiHeaders));
  }

  @Override
  public CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, boolean includeMetricType,
                                                            Map<String, String> okapiHeaders) {
    return fetchUcSettings(credentialsId, includeMetricType, okapiHeaders)
      .thenApply(this::convertResult);
  }

  @Override
  public CompletableFuture<UCSettingsKey> fetchKeyByCredentialsId(String credentialsId,
                                                                  Map<String, String> okapiHeaders) {
    return fetchDbUcSettings(credentialsId, okapiHeaders)
      .thenApply(this::convertToKey);
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, UCSettingsPatchRequest patchRequest,
                                        Map<String, String> okapiHeaders) {
    log.debug("update:: Update Uc Settings by credentialsId: {}, patchRequest data id: {}",
      credentialsId, patchRequest.getData().getId());

    return fetchDbUcSettings(credentialsId, okapiHeaders)
      .thenCombine(fetchUserInfo(okapiHeaders),
        (dbUcSettings, userInfo) -> prepareUpdate(dbUcSettings, patchRequest, userInfo))
      .thenCompose(dbUcSettings -> save(dbUcSettings, okapiHeaders))
      .thenApply(nothing());
  }

  @Override
  public CompletableFuture<UCSettings> save(String id, UCSettingsPostRequest request,
                                            Map<String, String> okapiHeaders) {
    log.info("save:: Save Uc Settings by id: {}, UCSettingsPostRequest data id: {}",
      id, request.getData().getId());

    updateRequest(request, id);
    return fetchUserInfo(okapiHeaders)
      .thenCombine(completedFuture(convertToDb(request)),
        (userInfo, dbUcSettings) -> prepareSave(dbUcSettings, userInfo))
      .thenCompose(dbUcSettings -> save(dbUcSettings, okapiHeaders))
      .thenApply(dbUCSettings -> new UcSettingsResult(dbUCSettings, null))
      .thenApply(this::convertResult);
  }

  private CompletableFuture<DbUcSettings> save(DbUcSettings ucSettings, Map<String, String> okapiHeaders) {
    log.debug("save:: Save Uc Settings by DbUcSettings: {}", ucSettings);
    CompletableFuture<DbUcSettings> result = new CompletableFuture<>();
    validate(ucSettings, okapiHeaders)
      .thenCompose(unused -> repository.save(ucSettings, tenantId(okapiHeaders)))
      .whenComplete(handleRepositoryException(result));
    return result;
  }

  private BiConsumer<DbUcSettings, Throwable> handleRepositoryException(CompletableFuture<DbUcSettings> result) {
    return (settings, throwable) -> {
      if (throwable == null) {
        log.info("UcSettings are successfully saved.");
        result.complete(settings);
      } else {
        if (throwable.getCause() instanceof ConstraintViolationException) {
          ConstraintViolationException cause = (ConstraintViolationException) throwable.getCause();
          Map<String, String> invalidValues = cause.getInvalidValues();
          String detailedMessage = invalidValues.entrySet().stream()
            .map(entry -> String.format("Value '%s' is invalid for '%s'.", entry.getValue(), entry.getKey()))
            .collect(Collectors.joining(" "));
          log.warn("handleRepositoryException:: Validation failed while saving UcSettings: {}", detailedMessage);
          InputValidationException exception = new InputValidationException("Invalid value", detailedMessage);
          result.completeExceptionally(exception);
        } else {
          log.warn("handleRepositoryException:: An exception occurred while saving UcSettings: {}",
            throwable.getMessage());
          result.completeExceptionally(throwable);
        }
      }
    };
  }

  private CompletableFuture<Void> validate(DbUcSettings ucSettings, Map<String, String> okapiHeaders) {
    log.info("validate:: Validating Uc Credentials before save");
    return getUcConfiguration(ucSettings, okapiHeaders)
      .thenCompose(ebscoClient::verifyCredentials)
      .thenAccept(b -> {
        if (Boolean.FALSE.equals(b)) {
          log.warn("Invalid UC Credentials");
          throw new InputValidationException("Invalid UC Credentials", null);
        }
        log.info("UC Credentials are valid");
      });
  }

  private CompletableFuture<UcConfiguration> getUcConfiguration(DbUcSettings ucSettings,
                                                                Map<String, String> okapiHeaders) {
    return authService.authenticate(okapiHeaders)
      .thenApply(authToken -> createConfiguration(ucSettings, authToken));
  }

  private UcConfiguration createConfiguration(DbUcSettings ucSettings, String authToken) {
    return UcConfiguration.builder().customerKey(ucSettings.getCustomerKey()).accessToken(authToken).build();
  }

  private DbUcSettings prepareUpdate(DbUcSettings dbUcSettings, UCSettingsPatchRequest patchRequest,
                                     UserInfo userInfo) {
    var patchAttributes = patchRequest.getData().getAttributes();
    log.info("prepareUpdate:: Setting changed fields to UcSettings for update");

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

  private CompletableFuture<DbUcSettings> fetchDbUcSettings(String credentialsId, Map<String, String> okapiHeaders) {
    log.info("fetchDbUcSettings:: by [credentialsId: {} ] for update.", credentialsId);
    return repository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(getUcSettingsOrFail(credentialsId));
  }

  private void updateRequest(UCSettingsPostRequest request, String credentialsId) {
    var attributes = request.getData().getAttributes();
    var platformType = ObjectUtils.defaultIfNull(attributes.getPlatformType(), PlatformType.ALL);
    var startMonth = ObjectUtils.defaultIfNull(attributes.getStartMonth(), Month.JAN);
    log.info("updateRequest:: Setting [credentialsId: {}, platformType: {}, startMonth: {} ] getAttributes fields to save",
      credentialsId, platformType, startMonth);

    attributes.setCredentialsId(credentialsId);
    attributes.setPlatformType(platformType);
    attributes.setStartMonth(startMonth);
  }

  private DbUcSettings prepareSave(DbUcSettings request, UserInfo userInfo) {
    return request.toBuilder()
      .createdDate(OffsetDateTime.now())
      .createdByUserId(toUUID(userInfo.getUserId()))
      .createdByUserName(userInfo.getUserName())
      .build();
  }

  private Function<Optional<DbUcSettings>, DbUcSettings> getUcSettingsOrFail(String credentialsId) {
    return dbUCSettings -> dbUCSettings
      .orElseThrow(() -> new NotFoundException(String.format(NOT_ENABLED_MESSAGE, credentialsId)));
  }

  private CompletableFuture<UcSettingsResult> fetchUcSettings(String credentialsId,
                                                              boolean includeMetricType,
                                                              Map<String, String> okapiHeaders) {
    return fetchDbUcSettings(credentialsId, okapiHeaders)
      .thenCompose(dbUCSettings ->
        fetchMetricTypeIfNeeded(includeMetricType, okapiHeaders, dbUCSettings)
          .thenApply(ucMetricType -> new UcSettingsResult(dbUCSettings, ucMetricType))
      );
  }

  private CompletableFuture<UcMetricType> fetchMetricTypeIfNeeded(boolean includeMetricType,
                                                                  Map<String, String> okapiHeaders,
                                                                  DbUcSettings dbUcSettings) {
    return includeMetricType
           ? getUcConfiguration(dbUcSettings, okapiHeaders).thenCompose(ebscoClient::getUsageMetricType)
           : CompletableFuture.completedFuture(null);
  }

  private UCSettingsKey convertToKey(DbUcSettings dbUcSettings) {
    return conversionService.convert(dbUcSettings, UCSettingsKey.class);
  }

  private DbUcSettings convertToDb(UCSettingsPostRequest request) {
    log.info("convertToDb:: Converting request with id: {} to DB object", request.getData().getId());
    return conversionService.convert(request, DbUcSettings.class);
  }

  private UCSettings convertResult(UcSettingsResult ucSettingsResult) {
    return conversionService.convert(ucSettingsResult, UCSettings.class);
  }
}
