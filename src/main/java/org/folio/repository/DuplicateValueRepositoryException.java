package org.folio.repository;

public class DuplicateValueRepositoryException extends RuntimeException {

  private static final String MESSAGE_PATTERN = "Duplicate %s";
  private static final String DETAILED_MESSAGE_PATTERN = "%s with %s '%s' already exist";

  private final String entity;
  private final String column;
  private final String value;

  public DuplicateValueRepositoryException(String entity, String column, String value) {
    this.entity = entity;
    this.column = column;
    this.value = value;
  }

  public DuplicateValueRepositoryException(Class<?> entityClass, String column, String value) {
    this(entityClass.getSimpleName(), column, value);
  }

  @Override
  public String getMessage() {
    return String.format(MESSAGE_PATTERN, column);
  }

  public String getDetailedMessage() {
    return String.format(DETAILED_MESSAGE_PATTERN, entity, column, value);
  }
}
