package org.folio.tag.repository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.folio.tag.RecordType;
import org.folio.tag.Tag;

public interface TagRepository {

  CompletableFuture<List<Tag>> findAll(String tenantId);

  CompletableFuture<List<Tag>> findByRecord(String tenantId, String recordId, RecordType recordType);

  CompletableFuture<List<Tag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes);

  CompletableFuture<Boolean> updateRecordTags(String tenantId, String recordId, RecordType recordType, List<String> tags);

  CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType);
  
}
