package org.folio.repository.holdings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(
  ignoreUnknown = true
)
@Getter
@Setter
@AllArgsConstructor
public class DbHolding {
  @JsonProperty("title_id")
  private final String titleId;
  @JsonProperty("package_id")
  private final String packageId;
  @JsonProperty("vendor_id")
  private final int vendorId;
  @JsonProperty("publication_title")
  private final String publicationTitle;
  @JsonProperty("publisher_name")
  private final String publisherName;
  @JsonProperty("resource_type")
  private final String resourceType;
}
