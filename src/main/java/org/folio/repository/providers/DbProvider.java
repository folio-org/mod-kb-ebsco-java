package org.folio.repository.providers;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DbProvider {
  String id;
  UUID credentialsId;
  String name;
}

