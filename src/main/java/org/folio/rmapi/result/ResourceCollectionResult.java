package org.folio.rmapi.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.Titles;
import org.folio.repository.holdings.DbHolding;
import org.folio.repository.resources.DbResource;

@Value
@AllArgsConstructor
public class ResourceCollectionResult {
  private Titles titles;
  private List<DbResource> titlesList;
  private List<DbHolding> holdings;
}
