package org.folio.repository.titles;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TitlesRepository {

  CompletableFuture<Void> save(DbTitle title, String tenantId);

  CompletableFuture<Void> delete(Long titleId, String credentialsId, String tenantId);

  CompletableFuture<List<DbTitle>> getTitlesByResourceTags(List<String> tags, int page, int count, String tenantId);

  CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, String tenantId);
}
