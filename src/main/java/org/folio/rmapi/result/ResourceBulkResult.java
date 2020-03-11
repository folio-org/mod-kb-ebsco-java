package org.folio.rmapi.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.Title;

@Value
@AllArgsConstructor
public class ResourceBulkResult {
  List<Title> titles;
  List<String> failedResources;
}
