package org.folio.rest.converter.kbcredentials;

import static java.util.Objects.requireNonNull;
import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;
import static org.folio.db.RowSetUtils.toUUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsKey;
import org.folio.rest.jaxrs.model.KbCredentialsKeyDataAttributes;
import org.folio.rest.jaxrs.model.Meta;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class KbCredentialsConverter {

  private KbCredentialsConverter() { }

  @Component("secured")
  @Primary
  public static class KbCredentialsFromDbSecuredConverter extends KbCredentialsFromDbNonSecuredConverter {

    public KbCredentialsFromDbSecuredConverter(@Value("${kb.ebsco.credentials.url.default}") String defaultUrl) {
      super(defaultUrl);
    }

    @Override
    public KbCredentials convert(@NotNull DbKbCredentials source) {
      return hideApiKey(requireNonNull(super.convert(source)));
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
      var dbKbCredentialsBuilder = DbKbCredentials.builder();

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

  @Component
  public static class KbCredentialsKeyConverter implements Converter<DbKbCredentials, KbCredentialsKey> {

    @Override
    public KbCredentialsKey convert(@NotNull DbKbCredentials source) {
      return new KbCredentialsKey()
        .withId(fromUUID(source.getId()))
        .withType(KbCredentialsKey.Type.KB_CREDENTIALS_KEY)
        .withAttributes(new KbCredentialsKeyDataAttributes()
          .withApiKey(source.getApiKey())
        );
    }
  }
}
