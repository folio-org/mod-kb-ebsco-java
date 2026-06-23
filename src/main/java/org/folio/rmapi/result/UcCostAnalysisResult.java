package org.folio.rmapi.result;

import java.util.List;
import java.util.Map;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.repository.holdings.DbHoldingInfo;

public record UcCostAnalysisResult(Map<String, UcCostAnalysis> costAnalysisMap, List<DbHoldingInfo>  holdingInfos)  { }
