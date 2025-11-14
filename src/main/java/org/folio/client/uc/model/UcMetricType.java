package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UcMetricType(int metricTypeId, String description) { }
