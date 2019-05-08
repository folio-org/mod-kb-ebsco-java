package org.folio.tag.repository.titles;

import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Title;

public interface TitlesRepository {

  CompletableFuture<Void> saveTitle(Title title, String tenantId);

  CompletableFuture<Void> deleteTitle(String titleId, String tenantId);
}
