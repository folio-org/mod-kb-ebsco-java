package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CommonUcConfiguration extends UcConfiguration {

  private final String fiscalYear;
  private final String fiscalMonth;
  private final String analysisCurrency;
}
