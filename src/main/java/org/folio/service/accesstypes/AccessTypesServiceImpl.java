package org.folio.service.accesstypes;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.FutureUtils;
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

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesServiceImpl.class);

  private static final String ACCESS_TYPES_LIMIT_PROP = "access.types.number.limit.value";
  private static final String MAXIMUM_ACCESS_TYPES_MESSAGE = "Maximum number of access types allowed is ";

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
      .thenApply(user -> setMetaInfo(Objects.requireNonNull(accessTypeToDbConverter.convert(requestData)), user))
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
    Future<Integer> configurationLimit = configuration.getInt(ACCESS_TYPES_LIMIT_PROP, accessTypesLimit,
      new OkapiParams(okapiHeaders));
    CompletableFuture<Integer> allowed = mapVertxFuture(configurationLimit);
    CompletableFuture<Integer> stored = repository.count(credentialsId, tenantId(okapiHeaders));
    return FutureUtils
      .allOfSucceeded(Arrays.asList(allowed, stored), throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::checkAccessTypesSize);
  }

  private Void checkAccessTypesSize(List<Integer> futures) {
    Integer configValue = futures.get(0);
    //do not allow user set access type more than defaultAccessTypesMaxValue
    int limit = configValue <= accessTypesLimit ? configValue : accessTypesLimit;
    Integer stored = futures.get(1);
    if (stored >= limit) {
      throw new BadRequestException(MAXIMUM_ACCESS_TYPES_MESSAGE + limit);
    }
    return null;
  }

  private DbAccessType setMetaInfo(DbAccessType dbAccessType, UserLookUp user) {
    return dbAccessType.toBuilder()
      .createdDate(Instant.now())
      .createdByUserId(user.getUserId())
      .createdByUsername(user.getUsername())
      .createdByFirstName(user.getFirstName())
      .createdByLastName(user.getLastName())
      .createdByMiddleName(user.getMiddleName())
      .build();
  }
}
