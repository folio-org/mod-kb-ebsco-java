package org.folio.repository.kbcredentials;

import java.util.UUID;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.folio.repository.DbMetadata;

@Getter
@SuperBuilder(toBuilder = true)
public class DbKbCredentials extends DbMetadata {

  UUID id;
  String name;
  String apiKey;
  String customerId;
  String url;
}
