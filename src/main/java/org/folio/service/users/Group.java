package org.folio.service.users;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class Group {

  private final String id;
  private final String group;
  private final String desc;
  private final Integer expirationOffsetInDays;
}
