package org.folio.rest.model.filter;

/**
 * Strategy interface for record-type-specific filter validation.
 */
@FunctionalInterface
public interface FilterValidator {

  void validate(Filter filter);
}
