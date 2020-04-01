package org.folio.rest.converter.kbcredentials;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    List<KbCredentials> kbCredentials = source.stream()
      .map(credentialsConverter::convert)
      .collect(Collectors.toList());
    return new KbCredentialsCollection()
      .withData(kbCredentials)
      .withMeta(new MetaTotalResults().withTotalResults(source.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
