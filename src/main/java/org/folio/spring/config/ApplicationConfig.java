package org.folio.spring.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.rest.converter",
  "org.folio.rest.parser",
  "org.folio.rest.validator",
  "org.folio.http",
  "org.folio.config.impl",
  "org.folio.config.cache",
  "org.folio.rest.util.template"})
public class ApplicationConfig {
  @Bean
  public PropertySourcesPlaceholderConfigurer placeholderConfigurer(){
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
}
