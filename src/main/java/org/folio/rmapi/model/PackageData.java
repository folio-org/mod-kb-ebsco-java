package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

  public Integer getPackageId() {
    return packageId;
  }

  public String getPackageName() {
    return packageName;
  }

  public Integer getVendorId() {
    return vendorId;
  }

  public String getVendorName() {
    return vendorName;
  }

  public Boolean getCustom() {
    return isCustom;
  }

  public Integer getTitleCount() {
    return titleCount;
  }

  public Boolean getSelected() {
    return isSelected;
  }

  public Integer getSelectedCount() {
    return selectedCount;
  }

  public String getContentType() {
    return contentType;
  }

  public VisibilityInfo getVisibilityData() {
    return visibilityData;
  }

  public CoverageDates getCustomCoverage() {
    return customCoverage;
  }

  public Boolean getTokenNeeded() {
    return isTokenNeeded;
  }

  public Boolean getAllowEbscoToAddTitles() {
    return allowEbscoToAddTitles;
  }

  public String getPackageType() {
    return packageType;
  }

  public void setCustom(Boolean custom) {
    isCustom = custom;
  }
}
