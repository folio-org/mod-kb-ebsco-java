package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TitlePost {
  @JsonProperty("titleName")
  private String titleName;

  @JsonProperty("edition")
  private String edition;

  @JsonProperty("publisherName")
  private String publisherName;

  @JsonProperty("pubType")
  private String pubType;

  @JsonProperty("description")
  private String description;

  @JsonProperty("isPeerReviewed")
  private boolean isPeerReviewed;

  @JsonProperty("identifiersList")
  private List<Identifier> identifiersList;

  @JsonProperty("contributorsList")
  private List<Contributor> contributorsList;
}
