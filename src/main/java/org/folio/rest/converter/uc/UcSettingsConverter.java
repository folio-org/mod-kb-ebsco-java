package org.folio.rest.converter.uc;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;
import static org.folio.db.RowSetUtils.toUUID;

import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.client.uc.model.UcMetricType;
import org.folio.repository.uc.DbUcSettings;
import org.folio.rest.jaxrs.model.Meta;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsKey;
import org.folio.rest.jaxrs.model.UCSettingsKeyDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rmapi.result.UcSettingsResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class UcSettingsConverter {

  private UcSettingsConverter() {
  }

  @Primary
  @Component("securedUcSettingsConverter")
  public static class FromDbSecuredConverter extends FromDbNonSecuredConverter {

    @Override
    public UCSettings convert(@NotNull DbUcSettings source) {
      UCSettings ucSettings = super.convert(source);
      if (ucSettings != null) {
        ucSettings.getAttributes().withCustomerKey(StringUtils.repeat("*", 40));
      }
      return ucSettings;
    }
  }

  @Component("nonSecuredUcSettingsConverter")
  public static class FromDbNonSecuredConverter implements Converter<DbUcSettings, UCSettings> {

    @Override
    public UCSettings convert(@NotNull DbUcSettings source) {
      return new UCSettings()
        .withId(fromUUID(source.getId()))
        .withType(UCSettings.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsDataAttributes()
          .withCredentialsId(fromUUID(source.getKbCredentialsId()))
          .withCustomerKey(source.getCustomerKey())
          .withCurrency(source.getCurrency())
          .withPlatformType(PlatformType.fromValue(source.getPlatformType()))
          .withStartMonth(Month.fromValue(source.getStartMonth()))
        )
        .withMeta(new Meta()
          .withCreatedByUserId(fromUUID(source.getCreatedByUserId()))
          .withUpdatedByUserId(fromUUID(source.getUpdatedByUserId()))
          .withCreatedByUsername(source.getCreatedByUserName())
          .withUpdatedByUsername(source.getUpdatedByUserName())
          .withCreatedDate(toDate(source.getCreatedDate()))
          .withUpdatedDate(toDate(source.getUpdatedDate()))
        );
    }
  }

  @Component
  public static class PostRequestToDbConverter implements Converter<UCSettingsPostRequest, DbUcSettings> {

    @Override
    public DbUcSettings convert(UCSettingsPostRequest ucSettingsPostRequest) {
      var attributes = ucSettingsPostRequest.getData().getAttributes();
      return DbUcSettings.builder()
        .customerKey(attributes.getCustomerKey())
        .kbCredentialsId(toUUID(attributes.getCredentialsId()))
        .currency(attributes.getCurrency().toUpperCase())
        .platformType(attributes.getPlatformType().value())
        .startMonth(attributes.getStartMonth().value())
        .build();
    }
  }

  @AllArgsConstructor
  public static class UcSettingsResultConverter implements Converter<UcSettingsResult, UCSettings> {

    private final Converter<DbUcSettings, UCSettings> fromDbConverter;
    private final Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper;

    @Override
    public UCSettings convert(UcSettingsResult ucSettingsResult) {
      UCSettings ucSettings = Objects.requireNonNull(fromDbConverter.convert(ucSettingsResult.getSettings()));
      UcMetricType ucMetricType = ucSettingsResult.getMetricType();
      if (ucMetricType != null) {
        UCSettingsDataAttributes.MetricType metricType = metricTypeMapper
          .getOrDefault(ucMetricType.getMetricTypeId(), UCSettingsDataAttributes.MetricType.UNKNOWN);
        ucSettings.getAttributes().setMetricType(metricType);
      }
      return ucSettings;
    }
  }

  @Component
  public static class UcSettingsKeyConverter implements Converter<DbUcSettings, UCSettingsKey> {

    @Override
    public UCSettingsKey convert(DbUcSettings source) {
      return new UCSettingsKey()
        .withId(fromUUID(source.getId()))
        .withType(UCSettingsKey.Type.UC_SETTINGS_KEY)
        .withAttributes(new UCSettingsKeyDataAttributes()
          .withCredentialsId(fromUUID(source.getKbCredentialsId()))
          .withCustomerKey(source.getCustomerKey())
        );
    }
  }
}
