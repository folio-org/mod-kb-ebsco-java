package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.ANY)
public class VendorById extends Vendor {

  private Proxy proxy;
  private TokenInfo vendorByIdToken;

  @JsonCreator
  @Builder(builderMethodName = "byIdBuilder")
  VendorById(@JsonProperty("vendorId") int vendorId, @JsonProperty("vendorName") String vendorName,
             @JsonProperty("packagesTotal") int packagesTotal, @JsonProperty("packagesSelected") int packagesSelected,
             @JsonProperty("isCustomer") boolean isCustomer, @JsonProperty("proxy") Proxy proxy,
             @JsonProperty("vendorToken") TokenInfo vendorByIdToken) {
    super(vendorId, vendorName, packagesTotal, packagesSelected, isCustomer, null);
    this.proxy = proxy;
    this.vendorByIdToken = vendorByIdToken;
  }

}
