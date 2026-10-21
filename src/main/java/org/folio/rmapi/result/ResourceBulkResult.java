package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Resource;

public record ResourceBulkResult(List<Resource> resources, List<String> failedResources) { }
