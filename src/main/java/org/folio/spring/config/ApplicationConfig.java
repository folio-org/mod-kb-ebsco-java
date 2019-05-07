package org.folio.spring.config;

import java.util.List;

import io.vertx.core.Vertx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;

import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.impl.ConfigurationClientProvider;
import org.folio.holdingsiq.service.impl.ConfigurationServiceCache;
import org.folio.holdingsiq.service.impl.ConfigurationServiceImpl;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.holdingsiq.service.validator.TitleParametersValidator;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.cache.VendorCacheKey;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.rest.converter",
  "org.folio.rest.parser",
  "org.folio.rest.validator",
  "org.folio.tag.repository",
  "org.folio.rest.util.template"})
public class ApplicationConfig {
  @Bean
  public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setLocation(new ClassPathResource("application.properties"));
    return configurer;
  }

  @Bean
  public ConversionService conversionService(List<Converter> converters) {
    DefaultConversionService conversionService = new DefaultConversionService();
    converters.forEach(conversionService::addConverter);
    return conversionService;
  }

  @Bean
  public VertxCache<String, org.folio.holdingsiq.model.Configuration> rmApiConfigurationCache(Vertx vertx, @Value("${configuration.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "rmApiConfigurationCache");
  }

  @Bean
  public VertxCache<VendorIdCacheKey, Long> vendorIdCache(Vertx vertx, @Value("${vendor.id.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "vendorIdCache");
  }

  @Bean
  public VertxCache<PackageCacheKey, PackageByIdData> packageCache(Vertx vertx, @Value("${package.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "packageCache");
  }

  @Bean
  public VertxCache<VendorCacheKey, VendorById> vendorCache(Vertx vertx, @Value("${vendor.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "vendorCache");
  }

  @Bean
  public VertxCache<ResourceCacheKey, Title> resourceCache(Vertx vertx, @Value("${resource.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "resourceCache");
  }

  @Bean
  public ConfigurationService configurationService(Vertx vertx, @Value("${configuration.cache.expire}") long expirationTime) {
    return new ConfigurationServiceCache(
      new ConfigurationServiceImpl(
        new ConfigurationClientProvider()), new VertxCache<>(vertx, expirationTime, "rmApiConfigurationCache"));
  }

  @Bean
  public TitleParametersValidator titleParametersValidator() {
    return new TitleParametersValidator();
  }

  @Bean
  public PackageParametersValidator packageParametersValidator() {
    return new PackageParametersValidator();
  }
}
