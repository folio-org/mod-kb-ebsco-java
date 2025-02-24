package org.folio.rest.converter.kbcredentials;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collection;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class KbCredentialsCollectionConverter {

  private KbCredentialsCollectionConverter() { }

  @Primary
  @Component("securedCredentialsCollection")
  public static class SecuredKbCredentialsCollectionConverter implements
    Converter<Collection<DbKbCredentials>, KbCredentialsCollection> {

    private final Converter<DbKbCredentials, KbCredentials> credentialsConverter;

    public SecuredKbCredentialsCollectionConverter(
      @Qualifier("secured") Converter<DbKbCredentials, KbCredentials> credentialsConverter) {
      this.credentialsConverter = credentialsConverter;
    }

    @Override
    public KbCredentialsCollection convert(@NonNull Collection<DbKbCredentials> source) {
      return new KbCredentialsCollection()
        .withData(mapItems(source, credentialsConverter::convert))
        .withMeta(new MetaTotalResults()
          .withTotalResults(source.size()))
        .withJsonapi(RestConstants.JSONAPI);
    }
  }

  @Component("nonSecuredCredentialsCollection")
  public static class NonSecuredKbCredentialsCollectionConverter implements
    Converter<Collection<DbKbCredentials>, KbCredentialsCollection> {

    private final Converter<DbKbCredentials, KbCredentials> nonSecuredCredentialsConverter;

    public NonSecuredKbCredentialsCollectionConverter(
      @Qualifier("nonSecured") Converter<DbKbCredentials, KbCredentials> nonSecuredCredentialsConverter) {
      this.nonSecuredCredentialsConverter = nonSecuredCredentialsConverter;
    }

    @Override
    public KbCredentialsCollection convert(@NonNull Collection<DbKbCredentials> source) {
      return new KbCredentialsCollection()
        .withData(mapItems(source, nonSecuredCredentialsConverter::convert))
        .withMeta(new MetaTotalResults()
          .withTotalResults(source.size()))
        .withJsonapi(RestConstants.JSONAPI);
    }
  }
}
