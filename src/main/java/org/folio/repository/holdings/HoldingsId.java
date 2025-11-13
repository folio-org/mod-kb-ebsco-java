package org.folio.repository.holdings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HoldingsId(@JsonProperty("title_id") String titleId,
                         @JsonProperty("package_id") String packageId,
                         @JsonProperty("vendor_id") int vendorId) { }
