package org.folio.service.accesstypes;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.AccessTypesMappingRepository;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
import org.folio.service.exc.ServiceExceptions;
import org.folio.service.userlookup.UserLookUpService;

@Component
public class AccessTypesServiceImpl implements AccessTypesService {

  @Autowired
  private UserLookUpService userLookUpService;
  @Autowired
  private AccessTypesRepository repository;
  @Autowired
  private AccessTypesMappingRepository mappingRepository;
  @Autowired
  private Converter<List<AccessTypeCollectionItem>, AccessTypeCollection> accessTypeCollectionConverter;

  @Value("${access.types.number.limit.value}")
  private int accessTypesMaxValue;

  @Override
  public CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType,
                                                          Map<String, String> okapiHeaders) {

    return validateAccessTypeLimit(okapiHeaders)
      .thenCompose(o -> userLookUpService.getUserInfo(okapiHeaders)
        .thenCompose(creatorUser -> {
          accessType.setCreator(
            getUserDisplayInfo(creatorUser.getFirstName(), creatorUser.getMiddleName(), creatorUser.getLastName()));
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
        accessType.setUpdater(
          getUserDisplayInfo(updaterUser.getFirstName(), updaterUser.getMiddleName(), updaterUser.getLastName()));
        accessType.getMetadata().setUpdatedByUsername(updaterUser.getUserName());
        return repository.update(id, accessType, tenantId(okapiHeaders));
      });
  }

  @Override
  public CompletableFuture<Void> assignToRecord(AccessTypeCollectionItem accessType, String recordId, RecordType recordType,
      Map<String, String> okapiHeaders) {

    AccessTypeMapping mapping = AccessTypeMapping.builder()
      .accessTypeId(accessType.getId())
      .recordId(recordId)
      .recordType(recordType).build();

    return mappingRepository.save(mapping, tenantId(okapiHeaders)).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> findByRecord(String recordId, RecordType recordType,
      Map<String, String> okapiHeaders) {
    return mappingRepository.findByRecord(recordId, recordType, tenantId(okapiHeaders))
      .thenApply(mapping -> mapping.orElseThrow(() -> new NotFoundException("asdasd")))
      .thenCompose(mapping -> {
        CompletableFuture<Optional<AccessTypeCollectionItem>> future = repository.findById(mapping.getAccessTypeId(),
          tenantId(okapiHeaders));

        return future.thenApply(getAccessTypeOrFail(mapping.getAccessTypeId()));
      });
  }

  private CompletableFuture<Void> validateAccessTypeLimit(Map<String, String> okapiHeaders) {
    return repository.count(tenantId(okapiHeaders))
      .thenApply(storedCount -> {
        if (storedCount >= accessTypesMaxValue) {
          throw new BadRequestException("Maximum number of access types allowed is " + accessTypesMaxValue);
        }
        return null;
      });
  }

  private UserDisplayInfo getUserDisplayInfo(String firstName, String middleName, String lastName) {
    final UserDisplayInfo userDisplayInfo = new UserDisplayInfo();
    userDisplayInfo.setFirstName(firstName);
    userDisplayInfo.setMiddleName(middleName);
    userDisplayInfo.setLastName(lastName);
    return userDisplayInfo;
  }

  private Function<Optional<AccessTypeCollectionItem>, AccessTypeCollectionItem> getAccessTypeOrFail(String id) {
    return accessType -> accessType.orElseThrow(() -> ServiceExceptions.notFound("Access type", id));
  }
}
