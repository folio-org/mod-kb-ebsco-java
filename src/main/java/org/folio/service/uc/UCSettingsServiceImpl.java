package org.folio.service.uc;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.NotFoundException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import org.folio.repository.uc.DbUCSettings;
import org.folio.repository.uc.UCSettingsRepository;
import org.folio.rest.jaxrs.model.UCSettings;

@Service
public class UCSettingsServiceImpl implements UCSettingsService {

  private static final String NOT_ENABLED_MESSAGE = "Usage Consolidation is not enabled for KB credentials with id [%s]";

  private final UCSettingsRepository repository;
  private final Converter<DbUCSettings, UCSettings> fromDbConverter;

  public UCSettingsServiceImpl(UCSettingsRepository repository,
                               Converter<DbUCSettings, UCSettings> converter) {
    this.repository = repository;
    this.fromDbConverter = converter;
  }

  @Override
  public CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return repository.findByCredentialsId(toUUID(credentialsId), tenantId(okapiHeaders))
      .thenApply(getUCSettingsOrFail(credentialsId));
  }

  private Function<Optional<DbUCSettings>, UCSettings> getUCSettingsOrFail(String credentialsId) {
    return dbUCSettings -> dbUCSettings.map(fromDbConverter::convert)
      .orElseThrow(() -> new NotFoundException(String.format(NOT_ENABLED_MESSAGE, credentialsId)));
  }
}
