package org.folio.rmapi.result;

import org.folio.client.uc.model.UcMetricType;
import org.folio.repository.uc.DbUcSettings;

public record UcSettingsResult(DbUcSettings settings, UcMetricType metricType) { }
