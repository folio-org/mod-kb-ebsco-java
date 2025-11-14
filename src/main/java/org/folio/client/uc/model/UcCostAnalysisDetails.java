package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UcCostAnalysisDetails(Double cost,
                                    Integer usage,
                                    Double costPerUse) { }
