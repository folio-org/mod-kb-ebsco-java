package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageData {
  @JsonProperty("packageId")
  private Integer packageId;
  @JsonProperty("packageName")
  private String packageName;
  @JsonProperty("vendorId")
  private Integer vendorId;
  @JsonProperty("vendorName")
  private String vendorName;
  @JsonProperty("isCustom")
  private Boolean isCustom;
  @JsonProperty("titleCount")
  private Integer titleCount;
  @JsonProperty("isSelected")
  private Boolean isSelected;
  @JsonProperty("selectedCount")
  private Integer selectedCount;
  @JsonProperty("contentType")
  private String contentType;
  @JsonProperty("visibilityData")
  private VisibilityInfo visibilityData;
  @JsonProperty("customCoverage")
  private CoverageDates customCoverage;
  @JsonProperty("isTokenNeeded")
  private Boolean isTokenNeeded;
  @JsonProperty("allowEbscoToAddTitles")
  private Boolean allowEbscoToAddTitles;
  @JsonProperty("packageType")
  private String packageType;
}
