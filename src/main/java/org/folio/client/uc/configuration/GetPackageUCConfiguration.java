package org.folio.client.uc.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class GetPackageUCConfiguration extends CommonUCConfiguration {

  private final boolean aggregatedFullText;
}
