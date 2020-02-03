package org.folio.service.accessTypes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.repository.accessTypes.AccessTypesRepository;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.userlookup.UserLookUp;
import org.folio.service.userlookup.UserLookUpService;

@Component
public class AccessTypesServiceImpl implements AccessTypesService {

  @Autowired
  private UserLookUpService userLookUpService;
  @Autowired
  private AccessTypesRepository repository;

  @Value("${access.types.number.limit.value}")
  private int accessTypesMaxValue;

  @Override
  public CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders) {

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

  private CompletableFuture<Void> validateAccessTypeLimit(Map<String, String> okapiHeaders) {
    return repository.count(TenantTool.tenantId(okapiHeaders))
      .thenApply(storedCount -> {
        if (storedCount >= accessTypesMaxValue){
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

  private CompletableFuture<AccessTypeCollectionItem> updateUserMetadata(UserLookUp creatorUser,
                                                                         AccessTypeCollectionItem accessType,
                                                                         Map<String, String> okapiHeaders) {
    accessType.getMetadata().setCreatedByUsername(creatorUser.getUserName());
    return repository.save(accessType, TenantTool.tenantId(okapiHeaders));
  }

}
