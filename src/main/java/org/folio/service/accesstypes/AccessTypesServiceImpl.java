package org.folio.service.accesstypes;

import static java.lang.String.format;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.TenantUtil.tenantId;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.folio.common.OkapiParams;
import org.folio.config.Configuration;
import org.folio.db.exc.DbExcUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.AccessTypePutRequest;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
import org.folio.rest.validator.AccessTypesBodyValidator;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.users.User;
import org.folio.service.users.UsersLookUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AccessTypesServiceImpl implements AccessTypesService {

  private static final String MAXIMUM_ACCESS_TYPES_MESSAGE = "Maximum number of access types allowed is %s";
  private static final String HAS_ASSIGNED_RECORDS_MESSAGE = "Can't delete access type that has assigned records";
  private static final String NOT_FOUND_BY_ID_MESSAGE = "Access type not found: id = %s";
  private static final String NOT_FOUND_BY_RECORD_MESSAGE = "Access type not found: recordId = %s, recordType = %s";

  @Autowired
  private UsersLookUpService usersLookUpService;
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
    return repository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(accessTypes -> mapItems(accessTypes, accessTypeFromDbConverter::convert))
      .thenCompose(accessTypes -> populateUserMetadata(okapiHeaders, accessTypes))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessType> findByUserAndId(String accessTypeId, Map<String, String> okapiHeaders) {
    return kbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(credentials -> findByCredentialsAndAccessTypeId(credentials.getId(), accessTypeId, true,
        okapiHeaders));
  }

  @Override
  public CompletableFuture<AccessType> findByCredentialsAndAccessTypeId(String credentialsId, String accessTypeId,
                                                                        boolean withMetadata,
                                                                        Map<String, String> okapiHeaders) {
    return fetchDbAccessType(credentialsId, accessTypeId, okapiHeaders)
      .thenApply(accessTypeFromDbConverter::convert)
      .thenCompose(accessType -> {
        if (withMetadata) {
          return populateUserMetadata(okapiHeaders, List.of(accessType))
            .thenApply(accessTypes -> accessTypes.get(0));
        } else {
          return CompletableFuture.completedFuture(accessType);
        }
      });
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames,
                                                             String credentialsId,
                                                             Map<String, String> okapiHeaders) {
    return repository.findByCredentialsAndNames(toUUID(credentialsId), accessTypeNames, tenantId(okapiHeaders))
      .thenApply(accessTypes -> mapItems(accessTypes, accessTypeFromDbConverter::convert))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessType> findByRecord(RecordKey recordKey, String credentialsId,
                                                    Map<String, String> okapiHeaders) {
    String recordId = recordKey.getRecordId();
    RecordType recordType = recordKey.getRecordType();
    return repository.findByCredentialsAndRecord(toUUID(credentialsId), recordId, recordType, tenantId(okapiHeaders))
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
      .thenApply(dbAccessType -> prePopulateCreatorMetadata(dbAccessType, okapiHeaders))
      .thenCompose(dbAccessType -> repository.save(dbAccessType, tenantId(okapiHeaders)))
      .thenApply(accessTypeFromDbConverter::convert)
      .thenCombine(usersLookUpService.lookUpUser(new OkapiParams(okapiHeaders)), this::setCreatorMetaInfo);
  }

  private DbAccessType prePopulateCreatorMetadata(DbAccessType dbAccessType, Map<String, String> okapiHeaders) {
    return dbAccessType.toBuilder()
      .createdByUserId(UUID.fromString(okapiHeaders.get(XOkapiHeaders.USER_ID)))
      .createdDate(OffsetDateTime.now())
      .build();
  }

  private DbAccessType prePopulateUpdaterMetadata(DbAccessType dbAccessType, Map<String, String> okapiHeaders) {
    return dbAccessType.toBuilder()
      .updatedByUserId(UUID.fromString(okapiHeaders.get(XOkapiHeaders.USER_ID)))
      .updatedDate(OffsetDateTime.now())
      .build();
  }

  @Override
  public CompletableFuture<Void> update(String credentialsId, String accessTypeId, AccessTypePutRequest putRequest,
                                        Map<String, String> okapiHeaders) {
    AccessType requestData = putRequest.getData();
    bodyValidator.validate(credentialsId, accessTypeId, requestData);
    return fetchDbAccessType(credentialsId, accessTypeId, okapiHeaders)
      .thenApply(accessType -> updateFields(accessType, requestData.getAttributes()))
      .thenApply(dbAccessType -> prePopulateUpdaterMetadata(dbAccessType, okapiHeaders))
      .thenCompose(dbAccessType -> repository.save(dbAccessType, tenantId(okapiHeaders)))
      .thenApply(accessType -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String accessTypeId, Map<String, String> okapiHeaders) {
    CompletableFuture<Void> resultFuture = new CompletableFuture<>();

    repository.delete(toUUID(credentialsId), toUUID(accessTypeId), tenantId(okapiHeaders))
      .whenComplete((v, throwable) -> {
        if (throwable != null) {
          Throwable cause = throwable.getCause();
          if (DbExcUtils.isFKViolation(throwable)) {
            resultFuture.completeExceptionally(new BadRequestException(HAS_ASSIGNED_RECORDS_MESSAGE));
          } else {
            resultFuture.completeExceptionally(cause);
          }
        } else {
          resultFuture.complete(v);
        }
      });

    return resultFuture;
  }

  @Override
  public CompletionStage<Map<String, DbAccessType>> findPerRecord(String credentialsId, ArrayList<String> recordIds,
                                                                  RecordType recordType, String tenant) {
    return repository.findPerRecord(credentialsId, recordIds, recordType, tenant);
  }

  private CompletableFuture<List<AccessType>> populateUserMetadata(Map<String, String> okapiHeaders,
                                                                   List<AccessType> accessTypes) {
    var usersIds = accessTypes.stream()
      .map(AccessType::getMetadata)
      .map(metadata -> Arrays.asList(metadata.getCreatedByUserId(), metadata.getUpdatedByUsername()))
      .flatMap(List::stream)
      .filter(Objects::nonNull)
      .distinct()
      .map(UUID::fromString)
      .collect(Collectors.toList());
    return usersLookUpService.lookUpUsers(usersIds, new OkapiParams(okapiHeaders))
      .thenApply(users -> users.stream().collect(Collectors.toMap(User::getId, u -> u)))
      .thenApply(usersMap -> enrichAccessTypes(accessTypes, usersMap));
  }

  private List<AccessType> enrichAccessTypes(List<AccessType> accessTypes, Map<String, User> usersMap) {
    accessTypes.forEach(accessType -> enrichAccessType(accessType, usersMap));
    return accessTypes;
  }

  private CompletableFuture<DbAccessType> fetchDbAccessType(String credentialsId, String accessTypeId,
                                                            Map<String, String> okapiHeaders) {
    return repository.findByCredentialsAndAccessTypeId(toUUID(credentialsId), toUUID(accessTypeId),
        tenantId(okapiHeaders))
      .thenApply(getAccessTypeOrFail(accessTypeId));
  }

  private CompletableFuture<Void> validateAccessTypeLimit(String credentialsId, Map<String, String> okapiHeaders) {
    return mapVertxFuture(configuration.getInt(configurationLimitCode, accessTypesLimit, new OkapiParams(okapiHeaders)))
      .thenCombine(repository.count(toUUID(credentialsId), tenantId(okapiHeaders)), this::checkStoredAccessTypesAmount);
  }

  private Void checkStoredAccessTypesAmount(int configLimit, int stored) {
    int limit = configLimit <= accessTypesLimit ? configLimit : accessTypesLimit;
    if (stored >= limit) {
      throw new BadRequestException(format(MAXIMUM_ACCESS_TYPES_MESSAGE, limit));
    }
    return null;
  }

  private void enrichAccessType(AccessType accessType, Map<String, User> usersMap) {
    var metadata = accessType.getMetadata();
    if (metadata.getCreatedByUserId() != null) {
      String creatorId = metadata.getCreatedByUserId();
      var user = usersMap.get(creatorId);
      setCreatorMetaInfo(accessType, user);
    }

    if (metadata.getUpdatedByUserId() != null) {
      String updaterId = metadata.getUpdatedByUserId();
      var user = usersMap.get(updaterId);
      setUpdaterMetaInfo(accessType, user);
    }
  }

  private AccessType setCreatorMetaInfo(AccessType accessType, User user) {
    if (user != null) {
      accessType.getMetadata().setCreatedByUsername(user.getUserName());
      accessType.setCreator(new UserDisplayInfo()
        .withFirstName(user.getFirstName())
        .withLastName(user.getLastName())
        .withMiddleName(user.getMiddleName()));
    }
    return accessType;
  }

  private AccessType setUpdaterMetaInfo(AccessType accessType, User user) {
    if (user != null) {
      accessType.getMetadata().setUpdatedByUsername(user.getUserName());
      accessType.setUpdater(new UserDisplayInfo()
        .withFirstName(user.getFirstName())
        .withLastName(user.getLastName())
        .withMiddleName(user.getMiddleName()));
    }
    return accessType;
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
