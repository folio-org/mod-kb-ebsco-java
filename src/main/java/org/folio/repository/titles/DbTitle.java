package org.folio.repository.titles;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.Title;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
