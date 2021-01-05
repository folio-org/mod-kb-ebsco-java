package org.folio.spring.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.holdingsiq.service.impl.ConfigurationClientProvider;
import org.folio.repository.uc.DbUCSettings;
import org.folio.repository.uc.UCSettingsRepository;
import org.folio.rest.converter.uc.UCSettingsConverter;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rmapi.result.UCSettingsResult;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.locale.LocaleSettingsService;
import org.folio.service.locale.LocaleSettingsServiceImpl;
import org.folio.service.uc.UCAuthService;
import org.folio.service.uc.UCSettingsService;
import org.folio.service.uc.UCSettingsServiceImpl;
import org.folio.service.uc.sorting.UCSortingComparatorProvider;
import org.folio.service.uc.sorting.UCSortingComparatorProviders;

@Configuration
public class UCConfig {

  @Bean
  public UCSettingsService securedUCSettingsService(KbCredentialsService nonSecuredCredentialsService,
                                                    UCAuthService authService, UCApigeeEbscoClient ebscoClient,
                                                    UCSettingsRepository repository,
                                                    Converter<UCSettingsResult, UCSettings> securedUCSettingsResultConverter,
                                                    Converter<UCSettingsPostRequest, DbUCSettings> toConverter) {
    return new UCSettingsServiceImpl(nonSecuredCredentialsService, repository, authService, ebscoClient,
      securedUCSettingsResultConverter, toConverter);
  }

  @Bean
  public UCSettingsService nonSecuredUCSettingsService(KbCredentialsService nonSecuredCredentialsService,
                                                       UCAuthService authService, UCApigeeEbscoClient ebscoClient,
                                                       UCSettingsRepository repository,
                                                       Converter<UCSettingsResult, UCSettings> nonSecuredUCSettingsResultConverter,
                                                       Converter<UCSettingsPostRequest, DbUCSettings> toConverter) {
    return new UCSettingsServiceImpl(nonSecuredCredentialsService, repository, authService, ebscoClient,
      nonSecuredUCSettingsResultConverter, toConverter);
  }

  @Bean
  public Converter<UCSettingsResult, UCSettings> securedUCSettingsResultConverter(
    Converter<DbUCSettings, UCSettings> securedUCSettingsConverter,
    Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper) {
    return new UCSettingsConverter.UCSettingsResultConverter(securedUCSettingsConverter, metricTypeMapper);
  }

  @Bean
  public Converter<UCSettingsResult, UCSettings> nonSecuredUCSettingsResultConverter(
    @Qualifier("nonSecuredUCSettingsConverter")
      Converter<DbUCSettings, UCSettings> nonSecuredUCSettingsConverter,
    Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper) {
    return new UCSettingsConverter.UCSettingsResultConverter(nonSecuredUCSettingsConverter, metricTypeMapper);
  }

  @Bean
  public Map<Integer, UCSettingsDataAttributes.MetricType> metricTypeMapper() {
    return Map.of(
      33, UCSettingsDataAttributes.MetricType.TOTAL,
      36, UCSettingsDataAttributes.MetricType.UNIQUE
    );
  }

  @Bean
  public UCSortingComparatorProvider<ResourceCostPerUseCollectionItem> resourceUCSortingComparatorProvider() {
    return UCSortingComparatorProviders.forResources();
  }

  @Bean
  public LocaleSettingsService localeSettingsService() {
    return new LocaleSettingsServiceImpl(new ConfigurationClientProvider());
  }
}
