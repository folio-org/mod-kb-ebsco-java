package org.folio.spring.config;

import java.util.Map;
import org.folio.client.uc.UcApigeeEbscoClient;
import org.folio.repository.uc.DbUcSettings;
import org.folio.repository.uc.UcSettingsRepository;
import org.folio.rest.converter.uc.UcSettingsConverter;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsKey;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rmapi.result.UcSettingsResult;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.locale.LocaleSettingsService;
import org.folio.service.locale.LocaleSettingsServiceImpl;
import org.folio.service.uc.UcAuthService;
import org.folio.service.uc.UcSettingsService;
import org.folio.service.uc.UcSettingsServiceImpl;
import org.folio.service.uc.sorting.UcSortingComparatorProvider;
import org.folio.service.uc.sorting.UcSortingComparatorProviders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class UcConfig {

  @Bean
  public UcSettingsService securedUcSettingsService(KbCredentialsService nonSecuredCredentialsService,
                                                    UcAuthService authService, UcApigeeEbscoClient ebscoClient,
                                                    UcSettingsRepository repository,
                                                    ConversionService securedUcConversionService) {
    return new UcSettingsServiceImpl(nonSecuredCredentialsService, repository, authService, ebscoClient,
      securedUcConversionService);
  }

  @Bean
  public UcSettingsService nonSecuredUcSettingsService(KbCredentialsService nonSecuredCredentialsService,
                                                       UcAuthService authService, UcApigeeEbscoClient ebscoClient,
                                                       UcSettingsRepository repository,
                                                       ConversionService nonSecuredUcConversionService) {
    return new UcSettingsServiceImpl(nonSecuredCredentialsService, repository, authService, ebscoClient,
      nonSecuredUcConversionService);
  }

  @Bean
  public Converter<UcSettingsResult, UCSettings> securedUcSettingsResultConverter(
    Converter<DbUcSettings, UCSettings> securedUcSettingsConverter,
    Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper) {
    return new UcSettingsConverter.UcSettingsResultConverter(securedUcSettingsConverter, metricTypeMapper);
  }

  @Bean
  public Converter<UcSettingsResult, UCSettings> nonSecuredUcSettingsResultConverter(
    @Qualifier("nonSecuredUCSettingsConverter")
    Converter<DbUcSettings, UCSettings> nonSecuredUcSettingsConverter,
    Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper) {
    return new UcSettingsConverter.UcSettingsResultConverter(nonSecuredUcSettingsConverter, metricTypeMapper);
  }

  @Bean
  public Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper() {
    return Map.of(
      33, UCSettingsDataAttributes.MetricType.TOTAL,
      36, UCSettingsDataAttributes.MetricType.UNIQUE
    );
  }

  @Bean
  public UcSortingComparatorProvider<ResourceCostPerUseCollectionItem> resourceUcSortingComparatorProvider() {
    return UcSortingComparatorProviders.forResources();
  }

  @Bean
  public LocaleSettingsService localeSettingsService() {
    return new LocaleSettingsServiceImpl();
  }

  @Bean
  public ConversionService nonSecuredUcConversionService(
    Converter<UcSettingsResult, UCSettings> nonSecuredUcSettingsResultConverter,
    Converter<UCSettingsPostRequest, DbUcSettings> postRequestConverter,
    Converter<DbUcSettings, UCSettingsKey> ucSettingsKeyConverter) {
    DefaultConversionService conversionService = new DefaultConversionService();
    conversionService.addConverter(nonSecuredUcSettingsResultConverter);
    conversionService.addConverter(postRequestConverter);
    conversionService.addConverter(ucSettingsKeyConverter);
    return conversionService;
  }

  @Bean
  public ConversionService securedUcConversionService(
    Converter<UcSettingsResult, UCSettings> securedUcSettingsResultConverter,
    Converter<UCSettingsPostRequest, DbUcSettings> postRequestConverter,
    Converter<DbUcSettings, UCSettingsKey> ucSettingsKeyConverter) {
    DefaultConversionService conversionService = new DefaultConversionService();
    conversionService.addConverter(securedUcSettingsResultConverter);
    conversionService.addConverter(postRequestConverter);
    conversionService.addConverter(ucSettingsKeyConverter);
    return conversionService;
  }
}
