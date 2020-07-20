package org.folio.repository.titles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.model.filter.TagFilter;

public interface TitlesRepository {

  CompletableFuture<Void> save(DbTitle title, String tenantId);

  CompletableFuture<Void> delete(Long titleId, UUID credentialsId, String tenantId);

  CompletableFuture<List<DbTitle>> getTitlesByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId);

  CompletableFuture<Integer> countTitlesByResourceTags(List<String> tags, UUID credentialsId, String tenantId);
}
