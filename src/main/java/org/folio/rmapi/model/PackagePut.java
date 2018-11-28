package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PackagePut {
  @JsonProperty("packageName")
  private String packageName;
  @JsonProperty(value = "contentType")
  private Integer contentType;
  @JsonProperty("customCoverage")
  private CoverageDates customCoverage;
  @JsonProperty("isSelected")
  private Boolean isSelected;
  @JsonProperty("allowEbscoToAddTitles")
  private Boolean allowEbscoToAddTitles;
  @JsonProperty("isHidden")
  private Boolean isHidden;
  @JsonProperty("packageToken")
  private TokenInfo packageToken;
  @JsonProperty("proxy")
  private Proxy proxy;
}
