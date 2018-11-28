package org.folio.rmapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
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

}
