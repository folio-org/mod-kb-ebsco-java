package org.folio.rest.converter.kbcredentials;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;

@Component
public class KbCredentialsCollectionConverter implements Converter<Collection<DbKbCredentials>, KbCredentialsCollection> {

  @Autowired
  private Converter<DbKbCredentials, KbCredentials> credentialsConverter;

  @Override
  public KbCredentialsCollection convert(@NotNull Collection<DbKbCredentials> source) {
    return new KbCredentialsCollection()
      .withData(mapItems(source, credentialsConverter::convert))
      .withMeta(new MetaTotalResults().withTotalResults(source.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
