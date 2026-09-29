package org.folio.service.sanitizer;

public interface Sanitizer<T> {

  T sanitize(T target);
}
