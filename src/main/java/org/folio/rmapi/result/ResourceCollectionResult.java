package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Titles;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.resources.DbResource;

public record ResourceCollectionResult(Titles titles, List<DbResource> titlesList, List<DbHoldingInfo> holdings) { }
