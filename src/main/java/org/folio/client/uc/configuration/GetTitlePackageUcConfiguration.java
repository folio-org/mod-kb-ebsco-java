package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class GetTitlePackageUcConfiguration extends CommonUcConfiguration {

  private final boolean publisherPlatform;
  private final boolean previousYear;
}
