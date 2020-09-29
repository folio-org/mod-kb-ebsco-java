package org.folio.client.uc.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UCTitleCostUsage {

  Map<String, UCPlatformUsage> platforms;
}
