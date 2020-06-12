package org.folio.repository.kbcredentials;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbKbCredentials {

  private final UUID id;
  private final String name;
  private final String apiKey;
  private final String customerId;
  private final String url;
  private final OffsetDateTime createdDate;
  private final OffsetDateTime updatedDate;
  private final UUID createdByUserId;
  private final UUID updatedByUserId;
  private final String createdByUserName;
  private final String updatedByUserName;
}
