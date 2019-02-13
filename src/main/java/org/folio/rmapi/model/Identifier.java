package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Identifier {

  @JsonProperty("id")
  public String id;

  /**
   * Sub-type for the identifier. This is used by only some identifiers to
   * designate Print, Online, etc. Empty=0, Print=1, Online=2, Preceding=3,
   * Succeeding=4, Regional=5, Linking=6 and Invalid=7
   *
   */
  @JsonProperty("subtype")
  public Integer subtype;

  /**
   * The type of identifier. The type of identifier. ISSN=0, ISBN=1, TSDID=2,
   * SPID=3, EjsJournaID=4, NewsbankID=5, ZDBID=6, EPBookID=7, Mid=8, or BHM=9
   *
   */
  @JsonProperty("type")
  public Integer type;

}
