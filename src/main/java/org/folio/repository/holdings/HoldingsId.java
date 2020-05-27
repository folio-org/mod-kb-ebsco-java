package org.folio.repository.holdings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
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
public class HoldingsId {
  @JsonProperty("title_id")
  private final String titleId;
  @JsonProperty("package_id")
  private final String packageId;
  @JsonProperty("vendor_id")
  private final int vendorId;
}
