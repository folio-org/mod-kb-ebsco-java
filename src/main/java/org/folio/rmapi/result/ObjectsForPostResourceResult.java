package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ObjectsForPostResourceResult {
  private Title title;
  private PackageByIdData packageData;
  private Titles titles;
}
