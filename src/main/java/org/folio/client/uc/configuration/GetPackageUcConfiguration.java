package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class GetPackageUcConfiguration extends CommonUcConfiguration {

  private final boolean aggregatedFullText;
}
