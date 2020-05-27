package org.folio.repository.providers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DbProvider {
  String id;
  String credentialsId;
  String name;
}

