package org.folio.rmapi.model;

import java.util.List;

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
public class ResourceData {
  @JsonProperty("titleId")
  private Integer titleId;
  @JsonProperty("isTitleCustom")
  private Boolean isTitleCustom;
  @JsonProperty("isSelected")
  private Boolean isSelected;
  @JsonProperty("visibilityData")
  private VisibilityInfo visibilityData;
  @JsonProperty("coverageStatement")
  private String coverageStatement;
  @JsonProperty("customEmbargoPeriod")
  private EmbargoPeriod customEmbargoPeriod;
  @JsonProperty("managedEmbargoPeriod")
  private EmbargoPeriod managedEmbargoPeriod;
  @JsonProperty("titleName")
  private String titleName;
  @JsonProperty("pubType")
  private String pubType;
  @JsonProperty("publisherName")
  private String publisherName;
  @JsonProperty("isPeerReviewed")
  private Boolean isPeerReviewed;
  @JsonProperty("description")
  private String description;
  @JsonProperty("edition")
  private String edition;
  @JsonProperty("proxy")
  private Proxy proxy;
  @JsonProperty("url")
  private String url;
  @JsonProperty("contributorsList")
  private List<Contributor> contributorsList;
  @JsonProperty("identifiersList")
  private List<Identifier> identifiersList;
  @JsonProperty("customCoverageList")
  private List<CoverageDates> customCoverageList;
  @JsonProperty("managedCoverageList")
  private List<CoverageDates> managedCoverageList;
  @JsonProperty("subjectsList")
  private List<Subject> subjectsList;
  @JsonProperty("isPackageCustom")
  private Boolean isPackageCustom;
  @JsonProperty("isTokenNeeded")
  private Boolean isTokenNeeded;
  @JsonProperty("locationId")
  private Integer locationId;
  @JsonProperty("packageId")
  private Integer packageId;
  @JsonProperty("packageName")
  private String packageName;
  @JsonProperty("vendorId")
  private Integer vendorId;
  @JsonProperty("vendorName")
  private String vendorName;
}
