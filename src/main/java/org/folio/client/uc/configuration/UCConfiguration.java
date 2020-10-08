package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class UCConfiguration {

  private final String customerKey;
  private final String accessToken;
}
