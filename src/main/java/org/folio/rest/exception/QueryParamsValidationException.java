package org.folio.rest.exception;

import java.io.Serial;
import java.util.List;
import lombok.Getter;

@Getter
public class QueryParamsValidationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final List<String> messageDetail;

  public QueryParamsValidationException(String message, List<String> messageDetail) {
    super(message);
    this.messageDetail = messageDetail;
  }
}
