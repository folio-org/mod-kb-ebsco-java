package org.folio.service.accesstypes;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.config.Configuration;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesRepository;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.userlookup.UserLookUpService;

@Component("newAccessTypesService")
public class AccessTypesServiceImpl implements AccessTypesService {

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
  private Converter<List<AccessType>, AccessTypeCollection> accessTypeCollectionConverter;
  @Autowired
  private Converter<DbAccessType, AccessType> accessTypeConverter;
  @Autowired
  private Configuration configuration;

  @Value("${kb.ebsco.credentials.access.types.limit}")
  private int defaultAccessTypesMaxValue;

  @Override
  public CompletableFuture<AccessTypeCollection> findByUser(Map<String, String> okapiHeaders) {
    return kbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(kbCredentials -> findByCredentialsId(kbCredentials.getId(), okapiHeaders));
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findByCredentialsId(String credentialsId,
                                                                     Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(credentialsId, tenantId(okapiHeaders))
      .thenApply(accessTypes -> mapItems(accessTypes, accessTypeConverter::convert))
      .thenApply(accessTypeCollectionConverter::convert);
  }

  @Override
  public CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames,
                                                             Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<AccessType> findById(String id, Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<AccessType> findByRecord(String recordId, RecordType recordType,
                                                    Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<AccessType> save(AccessType accessType,
                                            Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<Void> update(String id, AccessType accessType, Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
