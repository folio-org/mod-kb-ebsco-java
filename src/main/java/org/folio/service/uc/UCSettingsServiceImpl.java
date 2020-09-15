package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import org.folio.rest.jaxrs.model.UCSettings;

@Service
public class UCSettingsServiceImpl implements UCSettingsService {

  private final Converter<UCSettings, UCSettings> fromDbConverter;

  public UCSettingsServiceImpl(Converter<UCSettings, UCSettings> converter) {
    this.fromDbConverter = converter;
  }

  @Override
  public CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return null;
  }
}
