package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UcPlatformUsage(List<Integer> counts, Boolean publisherPlatform) { }
