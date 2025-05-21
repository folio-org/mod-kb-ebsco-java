package org.folio.common;

import io.vertx.sqlclient.Tuple;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Logger;

@UtilityClass
public class LogUtils {

  private static final String COUNT_LOG_MESSAGE = "Do count query = {} with params = {}";
  private static final String DELETE_LOG_MESSAGE = "Do delete query = {} with params = {}";
  private static final String INSERT_LOG_MESSAGE = "Do insert query = {} with params = {}";
  private static final String SELECT_LOG_MESSAGE = "Do select query = {} with params = {}";
  private static final String UPDATE_LOG_MESSAGE = "Do update query = {} with params = {}";
  private static final String SIZE_OF_LIST = "size of list ";


  public static void logCountQuery(Logger logger, String query) {
    logCountQuery(logger, query, Tuple.tuple());
  }

  public static void logCountQuery(Logger logger, String query, Tuple params) {
    logTraceLevel(logger, COUNT_LOG_MESSAGE, query, params, false);
  }

  public static void logUpdateQuery(Logger logger, String query, Tuple params) {
    logTraceLevel(logger, UPDATE_LOG_MESSAGE, query, params, false);
  }

  public static void logInsertQuery(Logger logger, String query) {
    logInsertQuery(logger, query, Tuple.tuple());
  }

  public static void logInsertQuery(Logger logger, String query, Tuple params) {
    logTraceLevel(logger, INSERT_LOG_MESSAGE, query, params, false);
  }

  public static void logInsertQuery(Logger logger, String query, Tuple params, boolean hideParams) {
    logTraceLevel(logger, INSERT_LOG_MESSAGE, query, params, hideParams);
  }

  public static void logDeleteQuery(Logger logger, String query) {
    logDeleteQuery(logger, query, Tuple.tuple());
  }

  public static void logDeleteQuery(Logger logger, String query, Tuple params) {
    logTraceLevel(logger, DELETE_LOG_MESSAGE, query, params, false);
  }

  public static void logSelectQuery(Logger logger, String query) {
    logSelectQuery(logger, query, Tuple.tuple());
  }

  public static void logSelectQuery(Logger logger, String query, Tuple params) {
    logTraceLevel(logger, SELECT_LOG_MESSAGE, query, params, false);
  }

  public static void logTraceLevel(Logger logger, String logMessage, String query, Tuple params, boolean hideParams) {
    var tuple = tupleToString(params, hideParams);
    logger.trace(logMessage, query, tuple);
  }

  private static String tupleToString(Tuple params, boolean hideParams) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int index = 0; index < params.size(); index++) {
      sb.append("{");
      sb.append(index + 1);
      sb.append(" = ");
      var paramsValue = hideParams ? "***" : params.getValue(index);
      if (paramsValue == null) {
        sb.append("NULL");
      } else {
        sb.append(paramsValue.getClass().getSimpleName());
        sb.append(": ");
        sb.append(paramsValue);
      }
      sb.append("}");
      if (index < params.size() - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Returns "0" if given collection is empty or null.
   *
   * @param input Collection of object
   * @return string of list size when items more than 3 - otherwise all items.
   */
  public static String collectionToLogMsg(Collection<?> input) {
    if (input == null || input.isEmpty()) {
      return SIZE_OF_LIST + 0;
    }
    return input.size() < 3
      ? input.toString()
      : SIZE_OF_LIST + input.size();
  }
}
