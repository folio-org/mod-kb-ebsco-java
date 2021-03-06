package org.folio.repository.holdings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@Builder(toBuilder = true)
public class DbHoldingInfo {

  @JsonProperty("title_id")
  private final int titleId;
  @JsonProperty("package_id")
  private final int packageId;
  @JsonProperty("vendor_id")
  private final int vendorId;
  @JsonProperty("publication_title")
  private final String publicationTitle;
  @JsonProperty("publisher_name")
  private final String publisherName;
  @JsonProperty("resource_type")
  private final String resourceType;
}
