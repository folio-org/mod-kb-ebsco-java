package org.folio.repository.providers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderInfoInDb {
  private String id;
  private String credentialsId;
  private String name;
}

