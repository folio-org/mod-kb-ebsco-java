package org.folio.common;

import io.vertx.sqlclient.Tuple;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Logger;

@UtilityClass
public class LogUtils {

  private static final String COUNT_LOG_MESSAGE = "Do count query = {} with params = {}";
  private static final String DELETE_LOG_MESSAGE = "Do delete query = {} with params = {}";
  private static final String INSERT_LOG_MESSAGE = "Do insert query = {} with params = {}";
  private static final String SELECT_LOG_MESSAGE = "Do select query = {} with params = {}";
  private static final String UPDATE_LOG_MESSAGE = "Do update query = {} with params = {}";

  public static void logCountQuery(Logger logger, String query) {
    logCountQuery(logger, query, Tuple.tuple());
  }

  public static void logCountQuery(Logger logger, String query, Tuple params) {
    logInfoLevel(logger, COUNT_LOG_MESSAGE, query, params);
  }

  public static void logUpdateQueryInfoLevel(Logger logger, String query) {
    logUpdateQueryInfoLevel(logger, query, Tuple.tuple());
  }

  public static void logUpdateQueryInfoLevel(Logger logger, String query, Tuple params) {
    logInfoLevel(logger, UPDATE_LOG_MESSAGE, query, params);
  }

  public static void logUpdateQueryDebugLevel(Logger logger, String query, Tuple params) {
    logDebugLevel(logger, UPDATE_LOG_MESSAGE, query, params);
  }

  public static void logInsertQueryInfoLevel(Logger logger, String query) {
    logInsertQueryInfoLevel(logger, query, Tuple.tuple());
  }

  public static void logInsertQueryInfoLevel(Logger logger, String query, Tuple params) {
    logInfoLevel(logger, INSERT_LOG_MESSAGE, query, params);
  }

  public static void logInsertQueryDebugLevel(Logger logger, String query, Tuple params) {
    logDebugLevel(logger, INSERT_LOG_MESSAGE, query, params);
  }

  public static void logDeleteQueryInfoLevel(Logger logger, String query) {
    logDeleteQueryInfoLevel(logger, query, Tuple.tuple());
  }

  public static void logDeleteQueryInfoLevel(Logger logger, String query, Tuple params) {
    logInfoLevel(logger, DELETE_LOG_MESSAGE, query, params);
  }

  public static void logDeleteQueryDebugLevel(Logger logger, String query) {
    logDeleteQueryDebugLevel(logger, query, Tuple.tuple());
  }

  public static void logDeleteQueryDebugLevel(Logger logger, String query, Tuple params) {
    logDebugLevel(logger, DELETE_LOG_MESSAGE, query, params);
  }

  public static void logSelectQueryInfoLevel(Logger logger, String query) {
    logSelectQueryInfoLevel(logger, query, Tuple.tuple());
  }

  public static void logSelectQueryInfoLevel(Logger logger, String query, Tuple params) {
    logInfoLevel(logger, SELECT_LOG_MESSAGE, query, params);
  }

  public static void logSelectQueryDebugLevel(Logger logger, String query) {
    logSelectQueryDebugLevel(logger, query, Tuple.tuple());
  }

  public static void logSelectQueryDebugLevel(Logger logger, String query, Tuple params) {
    logDebugLevel(logger, SELECT_LOG_MESSAGE, query, params);
  }

  public static void logInfoLevel(Logger logger, String logMessage, String query, Tuple params) {
    logger.info(logMessage, query, tupleToString(params));
  }

  public static void logDebugLevel(Logger logger, String logMessage, String query, Tuple params) {
    logger.debug(logMessage, query, tupleToString(params));
  }

  private static String tupleToString(Tuple params) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int index = 0; index < params.size(); index++) {
      sb.append("{");
      sb.append(index + 1);
      sb.append(" = ");
      var paramsValue = params.getValue(index);
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
}
