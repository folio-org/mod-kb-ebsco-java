package org.folio.service.accesstypes;

import static java.lang.String.format;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.config.Configuration;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.validator.AccessTypesBodyValidator;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.userlookup.UserLookUp;
import org.folio.service.userlookup.UserLookUpService;

@Component("newAccessTypesService")
public class AccessTypesServiceImpl implements AccessTypesService {

  private static final String MAXIMUM_ACCESS_TYPES_MESSAGE = "Maximum number of access types allowed is %s";

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
  public CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames,
                                                             Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<AccessType> findById(String id, Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<AccessType> findByRecord(String recordId, RecordType recordType,
                                                    Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<AccessType> save(String credentialsId, AccessTypePostRequest postRequest,
                                            Map<String, String> okapiHeaders) {
    AccessType requestData = postRequest.getData();
    bodyValidator.validate(credentialsId, requestData);
    return validateAccessTypeLimit(credentialsId, okapiHeaders)
      .thenCompose(o -> userLookUpService.getUserInfo(okapiHeaders))
      .thenApply(userInfo -> setMetaInfo(Objects.requireNonNull(accessTypeToDbConverter.convert(requestData)), userInfo))
      .thenCompose(dbAccessType -> repository.save(dbAccessType, tenantId(okapiHeaders)))
      .thenApply(accessTypeFromDbConverter::convert);
  }

  @Override
  public CompletableFuture<Void> update(String id, AccessType accessType, Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException();
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

  private DbAccessType setMetaInfo(DbAccessType dbAccessType, UserLookUp userInfo) {
    return dbAccessType.toBuilder()
      .createdDate(Instant.now())
      .createdByUserId(userInfo.getUserId())
      .createdByUsername(userInfo.getUsername())
      .createdByFirstName(userInfo.getFirstName())
      .createdByLastName(userInfo.getLastName())
      .createdByMiddleName(userInfo.getMiddleName())
      .build();
  }
}
