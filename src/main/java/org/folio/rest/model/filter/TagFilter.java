package org.folio.rest.model.filter;

import java.util.List;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.RecordType;

@Value
@Builder
public class TagFilter {

  List<String> tags;
  String recordIdPrefix;
  RecordType recordType;
  int offset;
  int count;
}
