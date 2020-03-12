package org.folio.rest.model.filter;

import java.util.List;

import lombok.Data;

import org.folio.repository.RecordType;

@Data
public class AccessTypeFilter {

  private List<String> accessTypeNames;
  private List<String> accessTypeIds;
  private String recordIdPrefix;
  private RecordType recordType;
  private int page;
  private int count;
}
