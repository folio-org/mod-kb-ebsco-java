package org.folio.rest.exception;

import java.io.Serial;
import java.util.List;

public class QueryParamsValidationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final List<String> messageDetail;

  public QueryParamsValidationException(List<String> messageDetail) {
    super("Query params validation failed.");
    this.messageDetail = messageDetail;
  }

  public List<String> getMessageDetail() {
    return messageDetail;
  }
}
