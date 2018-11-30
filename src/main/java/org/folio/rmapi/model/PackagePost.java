package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PackagePost {

  @JsonProperty("contentType")
  private int contentType;
  @JsonProperty("packageName")
  private String packageName;
  @JsonProperty("customCoverage")
  private CoverageDates coverage;
}
