package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;

public record ObjectsForPostResourceResult(Title title, PackageByIdData packageData, Titles titles) { }
