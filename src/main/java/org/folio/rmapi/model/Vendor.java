package org.folio.rmapi.model;

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
public class Vendor {
  @JsonProperty("vendorId")
  private int vendorId;
  @JsonProperty("vendorName")
  private String vendorName;
  @JsonProperty("packagesTotal")
  private int packagesTotal;
  @JsonProperty("packagesSelected")
  private int packagesSelected;
  @JsonProperty("isCustomer")
  private boolean isCustomer;
  @JsonProperty("vendorToken")
  private String vendorToken;

}
