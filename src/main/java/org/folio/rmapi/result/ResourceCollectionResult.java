package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Resources;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.resources.DbResource;

public record ResourceCollectionResult(Resources resources, List<DbResource> dbResources,
                                       List<DbHoldingInfo> holdings) { }
