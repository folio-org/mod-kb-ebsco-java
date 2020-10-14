package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class UCTitlePackageId {

  @JsonProperty("kbid")
  Integer titleId;
  @JsonProperty("listId")
  Integer packageId;
}
