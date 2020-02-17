package org.folio.service.accesstypes;

import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.common.FutureUtils;
import org.folio.common.OkapiParams;
import org.folio.config.Configuration;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.AccessTypesMappingRepository;
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

  private static final String ACCESS_TYPES_LIMIT_PROP = "access.types.number.limit.value";
  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesServiceImpl.class);

  @Autowired
  private UserLookUpService userLookUpService;
  @Autowired
  private AccessTypesRepository repository;
  @Autowired
  private AccessTypesMappingRepository mappingRepository;
  @Autowired
  private Converter<List<AccessTypeCollectionItem>, AccessTypeCollection> accessTypeCollectionConverter;
  @Autowired
  private Configuration configuration;

  @Value("${access.types.default.number.limit.value}")
  private int defaultAccessTypesMaxValue;

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
  public CompletableFuture<AccessTypeCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> findById(String id, Map<String, String> okapiHeaders) {
    return repository.findById(id, tenantId(okapiHeaders)).thenApply(getAccessTypeOrFail(id));
  }

  @Override
  public CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders) {
    return repository.delete(id, tenantId(okapiHeaders));
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
  public CompletableFuture<Void> updateRecordMapping(AccessTypeCollectionItem accessType, String recordId, RecordType recordType,
                                                     Map<String, String> okapiHeaders) {
    if (accessType == null) {
     return mappingRepository.deleteByRecord(recordId, recordType, tenantId(okapiHeaders));
    }

    return mappingRepository.findByRecord(recordId, recordType, tenantId(okapiHeaders))
      .thenCompose(dbMapping -> {
        AccessTypeMapping mapping;
        if (dbMapping.isPresent()) {
          mapping = dbMapping.get().toBuilder().accessTypeId(accessType.getId()).build();
        } else {
          mapping = getAccessTypeMapping(accessType, recordId, recordType);
        }
        return mappingRepository.save(mapping, tenantId(okapiHeaders)).thenApply(result -> null);
      });
  }

  private AccessTypeMapping getAccessTypeMapping(AccessTypeCollectionItem accessType, String recordId, RecordType recordType) {
    return AccessTypeMapping.builder()
      .id(UUID.randomUUID().toString())
      .accessTypeId(accessType.getId())
      .recordId(recordId)
      .recordType(recordType).build();
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> findByRecord(String recordId, RecordType recordType,
      Map<String, String> okapiHeaders) {
    return mappingRepository.findByRecord(recordId, recordType, tenantId(okapiHeaders))
      .thenApply(mapping -> mapping.orElseThrow(() -> new NotFoundException(
        String.format("Access type mapping not found: recordId = %s, recordType = %s", recordId, recordType)))
      )
      .thenCompose(mapping -> {
        CompletableFuture<Optional<AccessTypeCollectionItem>> future = repository.findById(mapping.getAccessTypeId(),
          tenantId(okapiHeaders));

        return future.thenApply(getAccessTypeOrFail(mapping.getAccessTypeId()));
      });
  }

  private CompletableFuture<Void> validateAccessTypeLimit(Map<String, String> okapiHeaders) {
    final CompletableFuture<Integer> allowed = mapVertxFuture(
      configuration.getInt(ACCESS_TYPES_LIMIT_PROP, defaultAccessTypesMaxValue, new OkapiParams(okapiHeaders)));
    final CompletableFuture<Integer> stored = repository.count(TenantTool.tenantId(okapiHeaders));
    return FutureUtils
      .allOfSucceeded(Arrays.asList(allowed, stored),throwable -> LOG.warn(throwable.getMessage(), throwable))
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
      throw new BadRequestException("Maximum number of access types allowed is " + limit);
    }
    return null;
  }
  private Function<Optional<AccessTypeCollectionItem>, AccessTypeCollectionItem> getAccessTypeOrFail(String id) {
    return accessType -> accessType.orElseThrow(() -> ServiceExceptions.notFound("Access type", id));
  }
}
