package org.folio.repository.kbcredentials;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbKbCredentials {

  private final String id;
  private final String name;
  private final String apiKey;
  private final String customerId;
  private final String url;
  private final Instant createdDate;
  private final Instant updatedDate;
  private final String createdByUserId;
  private final String updatedByUserId;
  private final String createdByUserName;
  private final String updatedByUserName;
}
