package org.folio.rest.converter.uc;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toDate;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.uc.DbUCSettings;
import org.folio.rest.jaxrs.model.Meta;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;

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
          .withPlatformType(UCSettingsDataAttributes.PlatformType.fromValue(source.getPlatformType()))
          .withStartMonth(UCSettingsDataAttributes.StartMonth.fromValue(source.getStartMonth()))
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
}
