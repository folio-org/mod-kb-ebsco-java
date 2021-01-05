package org.folio.service.locale;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocaleSettings {

  private final String locale;
  private final String timezone;
  private final String currency;

}
