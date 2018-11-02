package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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

  public String getDescription() {
    return description;
  }

  public String getEdition() {
    return edition;
  }

  public Boolean getPeerReviewed() {
    return isPeerReviewed;
  }

  public List<Contributor> getContributorsList() {
    return contributorsList;
  }

  public Integer getTitleId() {
    return titleId;
  }

  public String getTitleName() {
    return titleName;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public List<Identifier> getIdentifiersList() {
    return identifiersList;
  }

  public List<Subject> getSubjectsList() {
    return subjectsList;
  }

  public Boolean getTitleCustom() {
    return isTitleCustom;
  }

  public String getPubType() {
    return pubType;
  }

  public List<CustomerResources> getCustomerResourcesList() {
    return customerResourcesList;
  }
}
