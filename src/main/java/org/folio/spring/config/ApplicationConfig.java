package org.folio.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.rest.converter",
  "org.folio.rest.parser",
  "org.folio.rest.validator",
  "org.folio.http",
  "org.folio.config.impl",
  "org.folio.config.cache"})
public class ApplicationConfig {
  @Bean
  public PropertySourcesPlaceholderConfigurer placeholderConfigurer(){
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setLocation(new ClassPathResource("application.properties"));
    return configurer;
  }
}
