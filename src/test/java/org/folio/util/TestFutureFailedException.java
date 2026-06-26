package org.folio.util;

/**
 * Exception to indicate that a future operation has failed during test execution.
 * This runtime exception is intended to be thrown when asynchronous operations
 * represented by futures do not complete successfully.
 */
public class TestFutureFailedException extends RuntimeException {

  public TestFutureFailedException(Throwable cause) {
    super(cause);
  }
}
