package org.folio.repository.titles;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Title;

public interface TitlesRepository {

  CompletableFuture<Void> saveTitle(Title title, String tenantId);

  CompletableFuture<Void> deleteTitle(String titleId, String tenantId);

  CompletableFuture<List<DbTitle>> getTitlesByResourceTags(List<String> tags, int page, int count, String tenantId);

  CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, String tenantId);
}
