package org.folio.rmapi.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.Titles;

@Value
@AllArgsConstructor
public class ResourceBulkResult {
  Titles titles;
  List<String> failedResources;
}
