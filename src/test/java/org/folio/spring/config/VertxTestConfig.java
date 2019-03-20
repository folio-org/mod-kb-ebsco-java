package org.folio.spring.config;

import static org.mockito.Mockito.spy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import org.folio.rest.util.template.RMAPIServicesFactory;

@Configuration
@Import(ApplicationConfig.class)
public class VertxTestConfig {

  @Bean
  @Primary
  @Qualifier("spyRmApiServicesFactory")
  public RMAPIServicesFactory spyRmApiServicesFactory(@Qualifier("rmapiServicesFactory") RMAPIServicesFactory rmapiServicesFactory)
  {
    return spy(rmapiServicesFactory);
  }
}
