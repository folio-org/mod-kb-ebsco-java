package org.folio.rmapi.result;

import java.util.List;
import lombok.Value;
import org.folio.holdingsiq.model.Titles;

@Value
public class ResourceBulkResult {
  Titles titles;
  List<String> failedResources;
}
