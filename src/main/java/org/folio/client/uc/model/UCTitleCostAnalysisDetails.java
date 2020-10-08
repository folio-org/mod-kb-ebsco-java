package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UCTitleCostAnalysisDetails {

  Double cost;
  Integer usage;
  Double costPerUse;
}
