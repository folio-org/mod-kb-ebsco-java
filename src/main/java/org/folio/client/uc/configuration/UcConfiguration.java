package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class UcConfiguration {

  private final String customerKey;
  private final String accessToken;
}
