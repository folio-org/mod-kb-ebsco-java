package org.folio.service.kbcredentials;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;

@Component
public class KbCredentialsServiceImpl implements KbCredentialsService {

  @Autowired
  private KbCredentialsRepository repository;
  @Autowired
  private Converter<Collection<DbKbCredentials>, KbCredentialsCollection> collectionConverter;

  @Override
  public CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders))
      .thenApply(collectionConverter::convert);
  }

}
