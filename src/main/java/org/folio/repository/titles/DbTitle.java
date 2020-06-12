package org.folio.repository.titles;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.folio.holdingsiq.model.Title;

@Value
@Builder
public class DbTitle {
  @NonNull
  Long id;
  @NonNull
  UUID credentialsId;
  @NonNull
  String name;
  @Nullable
  Title title;
}
