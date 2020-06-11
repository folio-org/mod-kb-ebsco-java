package org.folio.rest.converter.kbcredentials;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;
import static org.folio.db.RowSetUtils.toUUID;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.Meta;

public class KbCredentialsConverter {

  private KbCredentialsConverter() {

  }

  @Component("secured")
  @Primary
  public static class KbCredentialsFromDbSecuredConverter extends KbCredentialsFromDbNonSecuredConverter {

    public KbCredentialsFromDbSecuredConverter(@Value("${kb.ebsco.credentials.url.default}") String defaultUrl) {
      super(defaultUrl);
    }

    @Override
    public KbCredentials convert(@NotNull DbKbCredentials source) {
      return hideApiKey(super.convert(source));
    }

    private KbCredentials hideApiKey(KbCredentials source) {
      source.getAttributes().withApiKey(StringUtils.repeat("*", 40));
      return source;
    }

  }

  @Component("nonSecured")
  public static class KbCredentialsFromDbNonSecuredConverter implements Converter<DbKbCredentials, KbCredentials> {

    private final String defaultUrl;

    public KbCredentialsFromDbNonSecuredConverter(@Value("${kb.ebsco.credentials.url.default}") String defaultUrl) {
      this.defaultUrl = defaultUrl;
    }

    @Override
    public KbCredentials convert(@NotNull DbKbCredentials source) {
      return new KbCredentials()
        .withId(fromUUID(source.getId()))
        .withType(KbCredentials.Type.KB_CREDENTIALS)
        .withAttributes(new KbCredentialsDataAttributes()
          .withApiKey(source.getApiKey())
          .withCustomerId(source.getCustomerId())
          .withName(source.getName())
          .withUrl(getUrl(source))
        )
        .withMeta(new Meta()
          .withCreatedByUserId(fromUUID(source.getCreatedByUserId()))
          .withCreatedByUsername(source.getCreatedByUserName())
          .withCreatedDate(toDate(source.getCreatedDate()))
          .withUpdatedByUserId(fromUUID(source.getUpdatedByUserId()))
          .withUpdatedByUsername(source.getUpdatedByUserName())
          .withUpdatedDate(toDate(source.getUpdatedDate()))
        );
    }

    private String getUrl(@NotNull DbKbCredentials source) {
      return source.getUrl() != null ? source.getUrl() : defaultUrl;
    }
  }

  @Component
  public static class KbCredentialsToDbConverter implements Converter<KbCredentials, DbKbCredentials> {

    @Override
    public DbKbCredentials convert(@NotNull KbCredentials source) {
      DbKbCredentials.DbKbCredentialsBuilder dbKbCredentialsBuilder = DbKbCredentials.builder();

      KbCredentialsDataAttributes attributes = source.getAttributes();
      return dbKbCredentialsBuilder
        .id(toUUID(source.getId()))
        .name(attributes.getName())
        .apiKey(attributes.getApiKey())
        .customerId(attributes.getCustomerId())
        .url(attributes.getUrl())
        .build();
    }
  }
}
