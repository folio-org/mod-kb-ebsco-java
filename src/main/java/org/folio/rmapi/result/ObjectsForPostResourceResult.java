package org.folio.rmapi.result;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;

@Value
@AllArgsConstructor
public class ObjectsForPostResourceResult {
  Title title;
  PackageByIdData packageData;
  Titles titles;
}
