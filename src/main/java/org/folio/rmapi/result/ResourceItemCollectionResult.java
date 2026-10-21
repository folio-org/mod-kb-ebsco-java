package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Facets;

public record ResourceItemCollectionResult(List<ResourceItemResult> items, Facets facets, Integer totalResults) { }
