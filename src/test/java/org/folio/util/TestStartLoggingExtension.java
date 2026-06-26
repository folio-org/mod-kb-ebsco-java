package org.folio.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class TestStartLoggingExtension implements BeforeTestExecutionCallback {

  private static final Logger LOG = LogManager.getLogger("Test");

  private TestStartLoggingExtension() { }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    LOG.info(
      "********** Running test method: {}.{} **********",
      context.getRequiredTestClass().getName(),
      context.getRequiredTestMethod().getName()
    );
  }
}
