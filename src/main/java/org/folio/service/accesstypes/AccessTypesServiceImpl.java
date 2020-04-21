package org.folio.service.accesstypes;

import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.BadRequestException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.FutureUtils;
import org.folio.common.OkapiParams;
import org.folio.config.Configuration;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.exc.ServiceExceptions;
import org.folio.service.userlookup.UserLookUp;
import org.folio.service.userlookup.UserLookUpService;

@Component
public class AccessTypesServiceImpl implements AccessTypesService {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesServiceImpl.class);

  private static final String ACCESS_TYPES_LIMIT_PROP = "access.types.number.limit.value";
  private static final String HAS_ASSIGNED_RECORDS_MESSAGE = "Can't delete access type that has assigned records";
  private static final String MAXIMUM_ACCESS_TYPES_MESSAGE = "Maximum number of access types allowed is ";

  @Autowired
  private UserLookUpService userLookUpService;
  @Autowired
  private AccessTypeMappingsService mappingService;
  @Autowired
  private AccessTypesRepository repository;
  @Autowired
  private Converter<List<AccessTypeCollectionItem>, AccessTypeCollection> accessTypeCollectionConverter;
  @Autowired
  private Configuration configuration;

  @Value("${kb.ebsco.credentials.access.types.limit}")
  private int defaultAccessTypesMaxValue;

  @Override
  public CompletableFuture<AccessTypeCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders))
      .thenCombine(mappingService.countRecordsByAccessType(okapiHeaders), this::setEachRecordUsage)
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames,
                                                             Map<String, String> okapiHeaders) {
    return repository.findByNames(accessTypeNames, tenantId(okapiHeaders))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> findById(String id, Map<String, String> okapiHeaders) {
    return repository.findById(id, tenantId(okapiHeaders))
      .thenApply(getAccessTypeOrFail(id))
      .thenCombine(mappingService.findByAccessTypeId(id, okapiHeaders), (accessType, accessTypeMappings) -> {
        accessType.setUsageNumber(accessTypeMappings.size());
        return accessType;
      });
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> findByRecord(String recordId, RecordType recordType,
                                                                  Map<String, String> okapiHeaders) {
    return mappingService.findByRecord(recordId, recordType, okapiHeaders)
      .thenCompose(mapping -> {
        CompletableFuture<Optional<AccessTypeCollectionItem>> future = repository.findById(mapping.getAccessTypeId(),
          tenantId(okapiHeaders));

        return future.thenApply(getAccessTypeOrFail(mapping.getAccessTypeId()));
      });
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType,
                                                          Map<String, String> okapiHeaders) {
    return validateAccessTypeLimit(okapiHeaders)
      .thenCompose(o -> userLookUpService.getUserInfo(okapiHeaders)
        .thenCompose(creatorUser -> {
          accessType.setCreator(getUserDisplayInfo(creatorUser));
          accessType.getMetadata().setCreatedByUsername(creatorUser.getUserName());
          return repository.save(accessType, tenantId(okapiHeaders));
        })
      );
  }

  @Override
  public CompletableFuture<Void> update(String id, AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders) {
    return userLookUpService.getUserInfo(okapiHeaders)
      .thenCompose(updaterUser -> {
        accessType.setUpdater(getUserDisplayInfo(updaterUser));
        accessType.getMetadata().setUpdatedByUsername(updaterUser.getUserName());
        return repository.update(id, accessType, tenantId(okapiHeaders));
      });
  }

  @Override
  public CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders) {
    return hasMappings(id, okapiHeaders)
      .thenAccept(hasMappings -> {
        if (BooleanUtils.isTrue(hasMappings)) {
          throw new BadRequestException(HAS_ASSIGNED_RECORDS_MESSAGE);
        }
      })
      .thenCompose(aVoid -> repository.delete(id, tenantId(okapiHeaders)));
  }

  private CompletableFuture<Boolean> hasMappings(String id, Map<String, String> okapiHeaders) {
    return mappingService.findByAccessTypeId(id, okapiHeaders)
      .thenApply(accessTypeMappings -> !accessTypeMappings.isEmpty());
  }

  private CompletableFuture<Void> validateAccessTypeLimit(Map<String, String> okapiHeaders) {
    final CompletableFuture<Integer> allowed = mapVertxFuture(
      configuration.getInt(ACCESS_TYPES_LIMIT_PROP, defaultAccessTypesMaxValue, new OkapiParams(okapiHeaders)));
    final CompletableFuture<Integer> stored = repository.count(TenantTool.tenantId(okapiHeaders));
    return FutureUtils
      .allOfSucceeded(Arrays.asList(allowed, stored), throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::checkAccessTypesSize);
  }

  private UserDisplayInfo getUserDisplayInfo(UserLookUp userLookUp) {
    final UserDisplayInfo userDisplayInfo = new UserDisplayInfo();
    userDisplayInfo.setFirstName(userLookUp.getFirstName());
    userDisplayInfo.setMiddleName(userLookUp.getMiddleName());
    userDisplayInfo.setLastName(userLookUp.getLastName());
    return userDisplayInfo;
  }

  private Void checkAccessTypesSize(List<Integer> futures) {
    final Integer configValue = futures.get(0);
    //do not allow user set access type more than defaultAccessTypesMaxValue
    final int limit = configValue <= defaultAccessTypesMaxValue ? configValue : defaultAccessTypesMaxValue;
    final Integer stored = futures.get(1);
    if (stored >= limit) {
      throw new BadRequestException(MAXIMUM_ACCESS_TYPES_MESSAGE + limit);
    }
    return null;
  }

  private Function<Optional<AccessTypeCollectionItem>, AccessTypeCollectionItem> getAccessTypeOrFail(String id) {
    return accessType -> accessType.orElseThrow(() -> ServiceExceptions.notFound("Access type", id));
  }

  private List<AccessTypeCollectionItem> setEachRecordUsage(List<AccessTypeCollectionItem> accessTypes,
                                                            Map<String, Integer> accessTypeMappingsCount) {
    accessTypes.forEach(accessType -> setRecordUsage(accessType, accessTypeMappingsCount));
    return accessTypes;
  }

  private void setRecordUsage(AccessTypeCollectionItem accessType, Map<String, Integer> accessTypeMappingsCount) {
    accessType.setUsageNumber(accessTypeMappingsCount.getOrDefault(accessType.getId(), 0));
  }
}
