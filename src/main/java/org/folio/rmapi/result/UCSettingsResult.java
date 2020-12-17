package org.folio.rmapi.result;

import lombok.Value;

import org.folio.client.uc.model.UCMetricType;
import org.folio.repository.uc.DbUCSettings;

@Value
public class UCSettingsResult {

  DbUCSettings settings;
  UCMetricType metricType;

}
