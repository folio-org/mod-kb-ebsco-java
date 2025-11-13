package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Titles;

public record ResourceBulkResult(Titles titles, List<String> failedResources) { }
