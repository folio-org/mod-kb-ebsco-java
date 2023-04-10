package org.folio.rmapi.result;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.Facets;

@Value
@Builder(toBuilder = true)
public class TitleCollectionResult {

  List<TitleResult> titleResults;
  Facets facets;
  Integer totalResults;
}
