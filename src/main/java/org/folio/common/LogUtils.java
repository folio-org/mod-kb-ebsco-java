package org.folio.common;

import io.vertx.core.logging.Logger;
import io.vertx.sqlclient.Tuple;

public final class LogUtils {

  private static final String COUNT_LOG_MESSAGE = "Do count query = {} with params = {}";
  private static final String DELETE_LOG_MESSAGE = "Do delete query = {} with params = {}";
  private static final String INSERT_LOG_MESSAGE = "Do insert query = {} with params = {}";
  private static final String SELECT_LOG_MESSAGE = "Do select query = {} with params = {}";
  private static final String UPDATE_LOG_MESSAGE = "Do update query = {} with params = {}";

  private LogUtils(){}

  public static void logCountQuery(Logger logger, String query) {
    logCountQuery(logger, query, Tuple.tuple());
  }

  public static void logCountQuery(Logger logger, String query, Tuple params) {
    logQuery(logger, COUNT_LOG_MESSAGE, query, params);
  }

  public static void logUpdateQuery(Logger logger, String query) {
    logUpdateQuery(logger, query, Tuple.tuple());
  }

  public static void logUpdateQuery(Logger logger, String query, Tuple params) {
    logQuery(logger, UPDATE_LOG_MESSAGE, query, params);
  }

  public static void logInsertQuery(Logger logger, String query) {
    logInsertQuery(logger, query, Tuple.tuple());
  }

  public static void logInsertQuery(Logger logger, String query, Tuple params) {
    logQuery(logger, INSERT_LOG_MESSAGE, query, params);
  }

  public static void logDeleteQuery(Logger logger, String query) {
    logDeleteQuery(logger, query, Tuple.tuple());
  }

  public static void logDeleteQuery(Logger logger, String query, Tuple params) {
    logQuery(logger, DELETE_LOG_MESSAGE, query, params);
  }

  public static void logSelectQuery(Logger logger, String query) {
    logSelectQuery(logger, query, Tuple.tuple());
  }

  public static void logSelectQuery(Logger logger, String query, Tuple params) {
    logQuery(logger, SELECT_LOG_MESSAGE, query, params);
  }

  public static void logQuery(Logger logger, String logMessage, String query, Tuple params) {
    logger.info(logMessage, query, tupleToString(params));
  }

  private static String tupleToString(Tuple params) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int index = 0; index < params.size(); index++) {
      Object paramsValue = params.getValue(index);
      sb.append("{");
      sb.append(index + 1);
      sb.append(" = ");
      if (paramsValue == null) {
        sb.append("NULL");
      } else {
        sb.append(paramsValue.getClass().getSimpleName());
        sb.append(": ");
        sb.append(paramsValue.toString());
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
