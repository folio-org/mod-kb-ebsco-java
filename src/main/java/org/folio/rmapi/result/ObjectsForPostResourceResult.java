package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Title;

public record ObjectsForPostResourceResult(Title title, PackageData packageData, boolean titleInPackage) { }
