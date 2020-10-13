package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CommonUCConfiguration extends UCConfiguration {

  private final String fiscalYear;
  private final String fiscalMonth;
  private final String analysisCurrency;
}
