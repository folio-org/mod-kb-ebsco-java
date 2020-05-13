package org.folio.service.accesstypes;

import static java.lang.String.format;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.config.Configuration;
import org.folio.db.exc.DbExcUtils;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.AccessTypePutRequest;
import org.folio.rest.validator.AccessTypesBodyValidator;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.userlookup.UserLookUp;
import org.folio.service.userlookup.UserLookUpService;

@Component
public class AccessTypesServiceImpl implements AccessTypesService {

  private static final String MAXIMUM_ACCESS_TYPES_MESSAGE = "Maximum number of access types allowed is %s";
  private static final String HAS_ASSIGNED_RECORDS_MESSAGE = "Can't delete access type that has assigned records";
  private static final String NOT_FOUND_BY_ID_MESSAGE = "Access type not found: id = %s";
  private static final String NOT_FOUND_BY_RECORD_MESSAGE = "Access type not found: recordId = %s, recordType = %s";

  @Autowired
  private UserLookUpService userLookUpService;
  @Autowired
  private AccessTypeMappingsService mappingService;
  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService kbCredentialsService;

  @Autowired
  private AccessTypesRepository repository;

  @Autowired
  private AccessTypesBodyValidator bodyValidator;

  @Autowired
  private Converter<List<AccessType>, AccessTypeCollection> accessTypeCollectionConverter;
  @Autowired
  private Converter<DbAccessType, AccessType> accessTypeFromDbConverter;
  @Autowired
  private Converter<AccessType, DbAccessType> accessTypeToDbConverter;
  @Autowired
  private Configuration configuration;

  @Value("${kb.ebsco.credentials.access.types.limit}")
  private int accessTypesLimit;
  @Value("${kb.ebsco.credentials.access.types.configuration.limit.code}")
  private String configurationLimitCode;

  @Override
  public CompletableFuture<AccessTypeCollection> findByUser(Map<String, String> okapiHeaders) {
    return kbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(kbCredentials -> findByCredentialsId(kbCredentials.getId(), okapiHeaders));
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findByCredentialsId(String credentialsId,
                                                                     Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(credentialsId, tenantId(okapiHeaders))
      .thenApply(accessTypes -> mapItems(accessTypes, accessTypeFromDbConverter::convert))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessType> findByUserAndId(String accessTypeId, Map<String, String> okapiHeaders) {
    return kbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(kbCredentials -> findByCredentialsAndAccessTypeId(kbCredentials.getId(), accessTypeId, okapiHeaders));
  }

  @Override
  public CompletableFuture<AccessType> findByCredentialsAndAccessTypeId(String credentialsId, String accessTypeId,
                                                                        Map<String, String> okapiHeaders) {
    return fetchDbAccessType(credentialsId, accessTypeId, okapiHeaders)
      .thenApply(accessTypeFromDbConverter::convert);
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames,
                                                             String credentialsId,
                                                             Map<String, String> okapiHeaders) {
    return repository.findByCredentialsAndNames(credentialsId, accessTypeNames, tenantId(okapiHeaders))
      .thenApply(accessTypes -> mapItems(accessTypes, accessTypeFromDbConverter::convert))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessType> findByRecord(RecordKey recordKey, String credentialsId,
                                                    Map<String, String> okapiHeaders) {
    String recordId = recordKey.getRecordId();
    RecordType recordType = recordKey.getRecordType();
    return repository.findByCredentialsAndRecord(credentialsId, recordId, recordType, tenantId(okapiHeaders))
      .thenApply(getAccessTypeOrFail(recordId, recordType))
      .thenApply(accessTypeFromDbConverter::convert);
  }

  @Override
  public CompletableFuture<AccessType> save(String credentialsId, AccessTypePostRequest postRequest,
                                            Map<String, String> okapiHeaders) {
    AccessType requestData = postRequest.getData();
    bodyValidator.validate(credentialsId, requestData);
    if (requestData.getAttributes().getCredentialsId() == null) {
      requestData.getAttributes().setCredentialsId(credentialsId);
    }
    return validateAccessTypeLimit(credentialsId, okapiHeaders)
      .thenApply(o -> accessTypeToDbConverter.convert(requestData))
      .thenCombine(userLookUpService.getUserInfo(okapiHeaders), this::setCreatorMetaInfo)
      .thenCompose(dbAccessType -> repository.save(dbAccessType, tenantId(okapiHeaders)))
      .thenApply(accessTypeFromDbConverter::convert);
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, String accessTypeId, AccessTypePutRequest putRequest,
                                        Map<String, String> okapiHeaders) {
    AccessType requestData = putRequest.getData();
    bodyValidator.validate(credentialsId, accessTypeId, requestData);
    return fetchDbAccessType(credentialsId, accessTypeId, okapiHeaders)
      .thenCombine(userLookUpService.getUserInfo(okapiHeaders), this::setUpdaterMetaInfo)
      .thenApply(accessType -> updateFields(accessType, requestData.getAttributes()))
      .thenCompose(dbAccessType -> repository.save(dbAccessType, tenantId(okapiHeaders)))
      .thenApply(accessType -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String accessTypeId, Map<String, String> okapiHeaders) {
    CompletableFuture<Void> resultFuture = new CompletableFuture<>();

    repository.delete(credentialsId, accessTypeId, tenantId(okapiHeaders))
      .whenComplete((aVoid, throwable) -> {
        if (throwable != null) {
          Throwable cause = throwable.getCause();
          if (DbExcUtils.isFKViolation(throwable)) {
            resultFuture.completeExceptionally(new BadRequestException(HAS_ASSIGNED_RECORDS_MESSAGE));
          } else {
            resultFuture.completeExceptionally(cause);
          }
        } else {
          resultFuture.complete(aVoid);
        }
      });

    return resultFuture;
  }

  private CompletableFuture<DbAccessType> fetchDbAccessType(String credentialsId, String accessTypeId,
                                                            Map<String, String> okapiHeaders) {
    return repository.findByCredentialsAndAccessTypeId(credentialsId, accessTypeId, tenantId(okapiHeaders))
      .thenApply(getAccessTypeOrFail(accessTypeId));
  }

  private CompletableFuture<Void> validateAccessTypeLimit(String credentialsId, Map<String, String> okapiHeaders) {
    return mapVertxFuture(configuration.getInt(configurationLimitCode, accessTypesLimit, new OkapiParams(okapiHeaders)))
      .thenCombine(repository.count(credentialsId, tenantId(okapiHeaders)), this::checkStoredAccessTypesAmount);
  }

  private Void checkStoredAccessTypesAmount(int configLimit, int stored) {
    int limit = configLimit <= accessTypesLimit ? configLimit : accessTypesLimit;
    if (stored >= limit) {
      throw new BadRequestException(format(MAXIMUM_ACCESS_TYPES_MESSAGE, limit));
    }
    return null;
  }

  private DbAccessType setCreatorMetaInfo(DbAccessType dbAccessType, UserLookUp userInfo) {
    return dbAccessType.toBuilder()
      .createdDate(Instant.now())
      .createdByUserId(userInfo.getUserId())
      .createdByUsername(userInfo.getUsername())
      .createdByFirstName(userInfo.getFirstName())
      .createdByLastName(userInfo.getLastName())
      .createdByMiddleName(userInfo.getMiddleName())
      .build();
  }

  private DbAccessType setUpdaterMetaInfo(DbAccessType dbAccessType, UserLookUp user) {
    return dbAccessType.toBuilder()
      .updatedDate(Instant.now())
      .updatedByUserId(user.getUserId())
      .updatedByUsername(user.getUsername())
      .updatedByFirstName(user.getFirstName())
      .updatedByLastName(user.getLastName())
      .updatedByMiddleName(user.getMiddleName())
      .build();
  }

  private DbAccessType updateFields(DbAccessType accessType, AccessTypeDataAttributes attributes) {
    return accessType.toBuilder()
      .name(attributes.getName())
      .description(attributes.getDescription())
      .build();
  }

  private Function<Optional<DbAccessType>, DbAccessType> getAccessTypeOrFail(String id) {
    return accessType -> accessType.orElseThrow(() -> new NotFoundException(
      String.format(NOT_FOUND_BY_ID_MESSAGE, id))
    );
  }

  private Function<Optional<DbAccessType>, DbAccessType> getAccessTypeOrFail(String recordId, RecordType recordType) {
    return accessType -> accessType.orElseThrow(() -> new NotFoundException(
      String.format(NOT_FOUND_BY_RECORD_MESSAGE, recordId, recordType))
    );
  }
}
