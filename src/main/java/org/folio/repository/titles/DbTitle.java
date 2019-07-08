package org.folio.repository.titles;

import lombok.Builder;
import lombok.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.folio.holdingsiq.model.Title;

@Value
@Builder
public class DbTitle {
  @NonNull
  private Long id;
  @NonNull
  private String name;
  @Nullable
  private Title title;
}
