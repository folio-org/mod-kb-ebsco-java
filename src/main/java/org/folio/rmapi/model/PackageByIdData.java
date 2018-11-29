package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.ANY)
public class PackageByIdData extends PackageData {

  private Proxy proxy;
  private TokenInfo packageToken;

  @JsonCreator
  @Builder(builderMethodName = "byIdBuilder")
  PackageByIdData(@JsonProperty("packageId") Integer packageId, @JsonProperty("packageName") String packageName,
                  @JsonProperty("vendorId") Integer vendorId, @JsonProperty("vendorName") String vendorName,
                  @JsonProperty("isCustom") Boolean isCustom, @JsonProperty("titleCount") Integer titleCount,
                  @JsonProperty("isSelected") Boolean isSelected, @JsonProperty("selectedCount") Integer selectedCount,
                  @JsonProperty("contentType") String contentType, @JsonProperty("visibilityData") VisibilityInfo visibilityData,
                  @JsonProperty("customCoverage") CoverageDates customCoverage, @JsonProperty("isTokenNeeded") Boolean isTokenNeeded,
                  @JsonProperty("allowEbscoToAddTitles") Boolean allowEbscoToAddTitles, @JsonProperty("packageType") String packageType,
                  @JsonProperty("proxy") Proxy proxy, @JsonProperty("packageToken") TokenInfo packageToken) {
    super(packageId, packageName, vendorId, vendorName, isCustom, titleCount, isSelected,
      selectedCount, contentType, visibilityData, customCoverage, isTokenNeeded, allowEbscoToAddTitles, packageType);
    this.proxy = proxy;
    this.packageToken = packageToken;
  }

  public PackageByIdData.PackageByIdDataBuilder toByIdBuilder() {
    return PackageByIdData.byIdBuilder()
      .allowEbscoToAddTitles(getAllowEbscoToAddTitles())
      .contentType(getContentType())
      .customCoverage(getCustomCoverage())
      .isCustom(getIsCustom())
      .isSelected(getIsSelected())
      .isTokenNeeded(getIsTokenNeeded())
      .packageId(getPackageId())
      .packageName(getPackageName())
      .packageToken(getPackageToken())
      .packageType(getPackageType())
      .proxy(getProxy())
      .selectedCount(getSelectedCount())
      .titleCount(getTitleCount())
      .vendorId(getVendorId())
      .vendorName(getVendorName())
      .visibilityData(getVisibilityData());
  }
}
