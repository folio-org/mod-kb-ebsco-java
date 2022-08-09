package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UcUsage {

  Map<String, UcPlatformUsage> platforms;
}
