package org.folio.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Configuration
@Import(ApplicationConfig.class)
public class TestConfig {

  @Bean
  public Vertx vertx(){
    //Initialize empty vertx object to be used by ApplicationConfig
    return Vertx.vertx();
  }

  @Bean
  public Context context(){
    //Initialize empty context
    return Vertx.vertx().getOrCreateContext();
  }
}
