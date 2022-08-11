package org.folio.rmapi.result;

import lombok.Value;
import org.folio.client.uc.model.UcMetricType;
import org.folio.repository.uc.DbUcSettings;

@Value
public class UcSettingsResult {

  DbUcSettings settings;
  UcMetricType metricType;

}
