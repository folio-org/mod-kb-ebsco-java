package org.folio.client.uc.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UCPlatformUsage {

  List<Integer> counts;
  Boolean publisherPlatform;
}
