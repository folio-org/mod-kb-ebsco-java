package org.folio.repository;

public class ForeignKeyNotFoundRepositoryException extends RuntimeException {

  private static final String MESSAGE_PATTERN = "%s not found by id: %s";

  private final String entity;
  private final String foreignKeyValue;

  public ForeignKeyNotFoundRepositoryException(String entity, String foreignKeyValue) {
    this.entity = entity;
    this.foreignKeyValue = foreignKeyValue;
  }

  public ForeignKeyNotFoundRepositoryException(Class<?> entityClass, String foreignKeyValue) {
    this(entityClass.getSimpleName(), foreignKeyValue);
  }

  @Override
  public String getMessage() {
    return String.format(MESSAGE_PATTERN, entity, foreignKeyValue);
  }
}
