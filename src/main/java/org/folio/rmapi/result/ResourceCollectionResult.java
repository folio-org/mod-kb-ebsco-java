package org.folio.rmapi.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.Titles;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.resources.ResourceInfoInDB;

@Value
@AllArgsConstructor
public class ResourceCollectionResult {
  private Titles titles;
  private List<ResourceInfoInDB> titlesList;
  private List<HoldingInfoInDB> holdings;
}
