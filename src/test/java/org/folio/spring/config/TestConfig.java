package org.folio.spring.config;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@Configuration
@Import(ApplicationConfig.class)
public class TestConfig {

  @Bean
  public Vertx vertx() {
    //Initialize empty vertx object to be used by ApplicationConfig
    return Vertx.vertx();
  }

  @Bean
  public Context context() {
    //Initialize empty context
    return Vertx.vertx().getOrCreateContext();
  }

  @Bean
  public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setLocation(new ClassPathResource("test-application.properties"));
    return configurer;
  }
}
