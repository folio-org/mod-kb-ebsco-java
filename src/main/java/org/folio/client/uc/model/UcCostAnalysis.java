package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UcCostAnalysis(UcCostAnalysisDetails current,
                             UcCostAnalysisDetails previous) { }
