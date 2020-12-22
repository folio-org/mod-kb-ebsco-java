package org.folio.rest.converter.uc;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;
import static org.folio.db.RowSetUtils.toUUID;

import java.util.Map;
import java.util.Objects;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.client.uc.model.UCMetricType;
import org.folio.repository.uc.DbUCSettings;
import org.folio.rest.jaxrs.model.Meta;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rmapi.result.UCSettingsResult;

public final class UCSettingsConverter {

  private UCSettingsConverter() {
  }

  @Primary
  @Component("securedUCSettingsConverter")
  public static class FromDbSecuredConverter extends FromDbNonSecuredConverter {

    @Override
    public UCSettings convert(@NotNull DbUCSettings source) {
      UCSettings ucSettings = super.convert(source);
      if (ucSettings != null) {
        ucSettings.getAttributes().withCustomerKey(StringUtils.repeat("*", 40));
      }
      return ucSettings;
    }
  }

  @Component("nonSecuredUCSettingsConverter")
  public static class FromDbNonSecuredConverter implements Converter<DbUCSettings, UCSettings> {

    @Override
    public UCSettings convert(@NotNull DbUCSettings source) {
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
  public static class PostRequestToDbConverter implements Converter<UCSettingsPostRequest, DbUCSettings> {

    @Override
    public DbUCSettings convert(UCSettingsPostRequest ucSettingsPostRequest) {
      var attributes = ucSettingsPostRequest.getData().getAttributes();
      return DbUCSettings.builder()
        .customerKey(attributes.getCustomerKey())
        .kbCredentialsId(toUUID(attributes.getCredentialsId()))
        .currency(attributes.getCurrency().toUpperCase())
        .platformType(attributes.getPlatformType().value())
        .startMonth(attributes.getStartMonth().value())
        .build();
    }
  }

  @AllArgsConstructor
  public static class UCSettingsResultConverter implements Converter<UCSettingsResult, UCSettings> {

    private final Converter<DbUCSettings, UCSettings> fromDbConverter;
    private final Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper;

    @Override
    public UCSettings convert(UCSettingsResult uCSettingsResult) {
      UCSettings ucSettings = Objects.requireNonNull(fromDbConverter.convert(uCSettingsResult.getSettings()));
      UCMetricType ucMetricType = uCSettingsResult.getMetricType();
      if (ucMetricType != null) {
        UCSettingsDataAttributes.MetricType metricType = metricTypeMapper
          .getOrDefault(ucMetricType.getMetricTypeId(), UCSettingsDataAttributes.MetricType.UNKNOWN);
        ucSettings.getAttributes().setMetricType(metricType);
      }
      return ucSettings;
    }
  }
}
