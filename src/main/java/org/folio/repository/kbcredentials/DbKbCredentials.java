package org.folio.repository.kbcredentials;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DbKbCredentials {

  String id;
  String name;
  String apiKey;
  String customerId;
  String url;
  Instant createdDate;
  Instant updatedDate;
  String createdByUserId;
  String updatedByUserId;
  String createdByUserName;
  String updatedByUserName;
}
