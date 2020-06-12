package org.folio.repository.tag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface TagRepository {

  CompletableFuture<List<DbTag>> findAll(String tenantId);

  CompletableFuture<List<DbTag>> findByRecord(String tenantId, String recordId, RecordType recordType);

  CompletableFuture<List<DbTag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes);

  CompletableFuture<List<DbTag>> findByRecordByIds(String tenantId, List<String> recordIds, RecordType recordType);

  CompletableFuture<Map<String, List<DbTag>>> findPerRecord(String tenantId, List<String> recordIds, RecordType recordType);

  CompletableFuture<Boolean> updateRecordTags(String tenantId, String recordId, RecordType recordType, List<String> tags);

  CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType);

  CompletableFuture<Integer> countRecordsByTags(List<String> tags, RecordType recordType, UUID credentialsId, String tenantId);

  CompletableFuture<Integer> countRecordsByTagsAndPrefix(List<String> tags, String recordIdPrefix, String tenantId, RecordType recordType);

  CompletableFuture<List<String>> findDistinctRecordTags(String tenantId);

  CompletableFuture<List<String>> findDistinctByRecordTypes(String tenantId, Set<RecordType> recordTypes);
}
