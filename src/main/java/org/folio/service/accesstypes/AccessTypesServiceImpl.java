package org.folio.service.accesstypes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesMappingRepository;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
import org.folio.rest.tools.utils.TenantTool;
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
          return repository.save(accessType, TenantTool.tenantId(okapiHeaders));
        })
      );
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(TenantTool.tenantId(okapiHeaders))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessTypeCollectionItem> findById(String id, Map<String, String> okapiHeaders) {
    return repository.findById(id, TenantTool.tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders) {
    return repository.delete(id, TenantTool.tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Void> update(String id, AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders) {

    return userLookUpService.getUserInfo(okapiHeaders)
      .thenCompose(updaterUser -> {
        accessType.setUpdater(
          getUserDisplayInfo(updaterUser.getFirstName(), updaterUser.getMiddleName(), updaterUser.getLastName()));
        accessType.getMetadata().setUpdatedByUsername(updaterUser.getUserName());
        return repository.update(id, accessType, TenantTool.tenantId(okapiHeaders));
      });
  }

  @Override
  public CompletableFuture<Boolean> existsById(String accessTypeId, Map<String, String> okapiHeaders) {
    return repository.existsById(accessTypeId, TenantTool.tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Void> assignAccessType(String accessTypeId, String recordId, RecordType recordType,
                                                  Map<String, String> okapiHeaders) {
    return findById(accessTypeId, okapiHeaders)
      .thenCompose(item -> mappingRepository.saveMapping(accessTypeId, recordId, recordType, okapiHeaders));
  }

  private CompletableFuture<Void> validateAccessTypeLimit(Map<String, String> okapiHeaders) {
    return repository.count(TenantTool.tenantId(okapiHeaders))
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

}
