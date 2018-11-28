package org.folio.rmapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Title {
  @JsonProperty("description")
  private String description;

  @JsonProperty("edition")
  private String edition;

  @JsonProperty("isPeerReviewed")
  private Boolean isPeerReviewed;

  @JsonProperty("contributorsList")
  private List<Contributor> contributorsList;

  @JsonProperty("titleId")
  private Integer titleId;

  @JsonProperty("titleName")
  private String titleName;

  @JsonProperty("publisherName")
  private String publisherName;

  @JsonProperty("identifiersList")
  private List<Identifier> identifiersList;

  @JsonProperty("subjectsList")
  private List<Subject> subjectsList;

  @JsonProperty("isTitleCustom")
  private Boolean isTitleCustom;

  @JsonProperty("pubType")
  private String pubType;

  @JsonProperty("customerResourcesList")
  private List<CustomerResources> customerResourcesList;

}
