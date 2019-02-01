package org.folio.spring.config;

import java.util.List;

import org.folio.config.RMAPIConfiguration;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.config.cache.VertxCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;

import io.vertx.core.Vertx;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.rest.converter",
  "org.folio.rest.parser",
  "org.folio.rest.validator",
  "org.folio.http",
  "org.folio.config.impl",
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
  public VertxCache<String, RMAPIConfiguration> rmApiConfigurationCache(Vertx vertx, @Value("${configuration.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "rmApiConfigurationCache");
  }

  @Bean
  public VertxCache<VendorIdCacheKey, Long> vendorIdCache(Vertx vertx, @Value("${vendor.id.cache.expire}") long vendorId) {
    return new VertxCache<>(vertx, vendorId, "vendorIdCache");
  }
}
