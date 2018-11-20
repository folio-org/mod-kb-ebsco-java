package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerResources {

  @JsonProperty("titleId")
  private Integer titleId;

  @JsonProperty("packageId")
  private Integer packageId;

  @JsonProperty("packageName")
  private String packageName;

  @JsonProperty("isPackageCustom")
  private Boolean isPackageCustom;

  @JsonProperty("vendorId")
  private Integer vendorId;

  @JsonProperty("vendorName")
  private String vendorName;

  @JsonProperty("locationId")
  private Integer locationId;

  @JsonProperty("isSelected")
  private Boolean isSelected;

  @JsonProperty("isTokenNeeded")
  private Boolean isTokenNeeded;

  @JsonProperty("packageType")
  private String packageType;

  @JsonProperty("visibilityData")
  private VisibilityInfo visibilityData;

  @JsonProperty("proxy")
  private Proxy proxy;
  
  @JsonProperty("managedCoverageList")
  private List<CoverageDates> managedCoverageList;

  @JsonProperty("customCoverageList")
  private List<CoverageDates> customCoverageList;

  @JsonProperty("coverageStatement")
  private String coverageStatement;

  @JsonProperty("managedEmbargoPeriod")
  private EmbargoPeriod managedEmbargoPeriod;

  @JsonProperty("customEmbargoPeriod")
  private EmbargoPeriod customEmbargoPeriod;

  @JsonProperty("url")
  private String url;

  @JsonProperty("userDefinedField1")
  private String userDefinedField1;
  @JsonProperty("userDefinedField2")
  private String userDefinedField2;
  @JsonProperty("userDefinedField3")
  private String userDefinedField3;
  @JsonProperty("userDefinedField4")
  private String userDefinedField4;
  @JsonProperty("userDefinedField5")
  private String userDefinedField5;

  public Integer getTitleId() {
    return titleId;
  }

  public Integer getPackageId() {
    return packageId;
  }

  public String getPackageName() {
    return packageName;
  }

  public Boolean getPackageCustom() {
    return isPackageCustom;
  }

  public Integer getVendorId() {
    return vendorId;
  }

  public String getVendorName() {
    return vendorName;
  }

  public Integer getLocationId() {
    return locationId;
  }

  public Boolean getSelected() {
    return isSelected;
  }

  public Boolean getTokenNeeded() {
    return isTokenNeeded;
  }

  public VisibilityInfo getVisibilityData() {
    return visibilityData;
  }
  
  public Proxy getProxy() {
    return proxy;
  }

  public List<CoverageDates> getManagedCoverageList() {
    return managedCoverageList;
  }

  public List<CoverageDates> getCustomCoverageList() {
    return customCoverageList;
  }

  public String getCoverageStatement() {
    return coverageStatement;
  }

  public EmbargoPeriod getManagedEmbargoPeriod() {
    return managedEmbargoPeriod;
  }

  public EmbargoPeriod getCustomEmbargoPeriod() {
    return customEmbargoPeriod;
  }

  public String getUrl() {
    return url;
  }

  public String getUserDefinedField1() {
    return userDefinedField1;
  }

  public String getUserDefinedField2() {
    return userDefinedField2;
  }

  public String getUserDefinedField3() {
    return userDefinedField3;
  }

  public String getUserDefinedField4() {
    return userDefinedField4;
  }

  public String getUserDefinedField5() {
    return userDefinedField5;
  }

  public String getPackageType() {
    return packageType;
  }
}
