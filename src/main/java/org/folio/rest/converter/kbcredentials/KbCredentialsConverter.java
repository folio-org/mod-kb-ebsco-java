package org.folio.rest.converter.kbcredentials;

import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.Meta;

public class KbCredentialsConverter {

  private KbCredentialsConverter() {

  }

  @Component
  public static class KbCredentialsFromDbConverter implements Converter<DbKbCredentials, KbCredentials> {

    private final String defaultUrl;

    public KbCredentialsFromDbConverter(@Value("${kb.ebsco.credentials.url.default}") String defaultUrl) {
      this.defaultUrl = defaultUrl;
    }

    @Override
    public KbCredentials convert(@NotNull DbKbCredentials source) {
      return new KbCredentials()
        .withId(source.getId())
        .withType(KbCredentials.Type.KB_CREDENTIALS)
        .withAttributes(new KbCredentialsDataAttributes()
          .withApiKey(hideApiKey(source))
          .withCustomerId(source.getCustomerId())
          .withName(source.getName())
          .withUrl(getUrl(source))
        )
        .withMeta(new Meta()
          .withCreatedByUserId(source.getCreatedByUserId())
          .withCreatedByUsername(source.getCreatedByUserName())
          .withCreatedDate(toDate(source.getCreatedDate()))
          .withUpdatedByUserId(source.getUpdatedByUserId())
          .withUpdatedByUsername(source.getUpdatedByUserName())
          .withUpdatedDate(toDate(source.getUpdatedDate()))
        );
    }

    private String getUrl(@NotNull DbKbCredentials source) {
      return source.getUrl() != null ? source.getUrl() : defaultUrl;
    }

    private String hideApiKey(@NotNull DbKbCredentials source) {
      return source.getApiKey() != null ? StringUtils.repeat("*", 40) : null;
    }

    private Date toDate(Instant date) {
      return date != null ? Date.from(date) : null;
    }
  }

  @Component
  public static class KbCredentialsToDbConverter implements Converter<KbCredentials, DbKbCredentials> {

    @Override
    public DbKbCredentials convert(@NotNull KbCredentials source) {
      DbKbCredentials.DbKbCredentialsBuilder dbKbCredentialsBuilder = DbKbCredentials.builder();

      KbCredentialsDataAttributes attributes = source.getAttributes();
      return dbKbCredentialsBuilder
        .id(source.getId())
        .name(attributes.getName())
        .apiKey(attributes.getApiKey())
        .customerId(attributes.getCustomerId())
        .url(attributes.getUrl())
        .build();
    }
  }
}
