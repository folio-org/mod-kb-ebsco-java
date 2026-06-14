package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;

public record ObjectsForPostResourceResult(Title title, PackageData packageData, Titles titles) { }
