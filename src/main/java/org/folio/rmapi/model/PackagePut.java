package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public Integer getContentType() {
    return contentType;
  }

  public void setContentType(Integer contentType) {
    this.contentType = contentType;
  }

  public CoverageDates getCustomCoverage() {
    return customCoverage;
  }

  public void setCustomCoverage(CoverageDates customCoverage) {
    this.customCoverage = customCoverage;
  }

  @JsonIgnore
  public Boolean getSelected() {
    return isSelected;
  }

  public void setSelected(Boolean selected) {
    isSelected = selected;
  }

  public Boolean getAllowEbscoToAddTitles() {
    return allowEbscoToAddTitles;
  }

  public void setAllowEbscoToAddTitles(Boolean allowEbscoToAddTitles) {
    this.allowEbscoToAddTitles = allowEbscoToAddTitles;
  }

  @JsonIgnore
  public Boolean getHidden() {
    return isHidden;
  }

  public void setHidden(Boolean hidden) {
    isHidden = hidden;
  }

  public TokenInfo getPackageToken() {
    return packageToken;
  }

  public void setPackageToken(TokenInfo packageToken) {
    this.packageToken = packageToken;
  }

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }
}
