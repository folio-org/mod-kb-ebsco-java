package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Packages {

  @JsonProperty("totalResults")
  private Integer totalResults;
  @JsonProperty("packagesList")
  private List<PackageData> packagesList;

  public Integer getTotalResults() {
    return totalResults;
  }

  public void setTotalResults(Integer totalResults) {
    this.totalResults = totalResults;
  }

  public List<PackageData> getPackagesList() {
    return packagesList;
  }

  public void setPackagesList(List<PackageData> packagesList) {
    this.packagesList = packagesList;
  }

}
