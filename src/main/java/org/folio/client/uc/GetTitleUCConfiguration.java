package org.folio.client.uc;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class GetTitleUCConfiguration extends UCConfiguration {

  private final String fiscalYear;
  private final String fiscalMonth;
  private final String analysisCurrency;
  private final boolean aggregatedFullText;
}
