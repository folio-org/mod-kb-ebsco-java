package org.folio.rmapi.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.Titles;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.resources.DbResource;

@Value
@AllArgsConstructor
public class ResourceCollectionResult {
  Titles titles;
  List<DbResource> titlesList;
  List<DbHoldingInfo> holdings;
}
