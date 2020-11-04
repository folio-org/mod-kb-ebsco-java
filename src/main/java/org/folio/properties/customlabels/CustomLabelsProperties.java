package org.folio.properties.customlabels;

import lombok.Value;
import org.springframework.stereotype.Component;

@Value
@Component
public class CustomLabelsProperties {

  int labelMaxLength;
}
