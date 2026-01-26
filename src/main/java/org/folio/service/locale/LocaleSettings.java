package org.folio.service.locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocaleSettings {

  private final String locale;
  private final String timezone;
  private final String currency;
}
