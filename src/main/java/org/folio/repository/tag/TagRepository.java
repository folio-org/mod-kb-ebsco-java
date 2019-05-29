package org.folio.repository.tag;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface TagRepository {

  CompletableFuture<List<Tag>> findAll(String tenantId);

  CompletableFuture<List<Tag>> findByRecord(String tenantId, String recordId, RecordType recordType);

  CompletableFuture<List<Tag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes);

  CompletableFuture<Boolean> updateRecordTags(String tenantId, String recordId, RecordType recordType, List<String> tags);

  CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType);

  CompletableFuture<Integer> countRecordsByTags(List<String> tags, String tenantId, RecordType recordType);

  CompletableFuture<Integer> countRecordsByTagsAndPrefix(List<String> tags, String recordIdPrefix, String tenantId, RecordType recordType);
}
