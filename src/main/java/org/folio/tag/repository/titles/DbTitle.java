package org.folio.tag.repository.titles;

import org.folio.holdingsiq.model.Title;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Value;

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
