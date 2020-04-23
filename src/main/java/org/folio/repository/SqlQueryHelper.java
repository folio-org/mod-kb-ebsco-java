package org.folio.repository;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.folio.common.ListUtils;

public final class SqlQueryHelper {

  private SqlQueryHelper() {

  }

  public static String selectQuery(String... columns) {
    if (columns.length == 0) {
      return "SELECT * FROM %s t1";
    } else {
      return "SELECT " + joinWithComa(columns) + " FROM %s";
    }
  }

  public static String insertQuery(String... columns) {
    return "INSERT INTO %s (" + joinWithComa(columns) + ") VALUES "
      + ListUtils.createInsertPlaceholders(columns.length, 1);
  }

  public static String deleteQuery() {
    return "DELETE FROM %s";
  }

  public static String leftJoinQuery(String query, String columnT1, String columnT2) {
    return "LEFT JOIN (" + query + ") t2 ON t1." + columnT1 + " = t2." + columnT2;
  }

  public static String count(String expression, String asColumn) {
    return "COUNT(" + expression + ") as " + asColumn;
  }

  public static String groupByQuery(String... columns) {
    return "GROUP BY " + joinWithComa(columns);
  }

  public static String whereQuery(String... columns) {
    String whereQuery = Arrays.stream(columns)
      .map(SqlQueryHelper::assignParameter)
      .collect(Collectors.joining(" AND "));
    return "WHERE " + whereQuery;
  }

  public static String updateQuery(String... columns) {
    return "UPDATE %s SET " + assignParameters(columns);
  }

  public static String updateOnConflictedIdQuery(String idColumnName, String... columns) {
    String updateColumns = Arrays.stream(columns)
      .filter(columnName -> !columnName.equals(idColumnName))
      .map(SqlQueryHelper::assignExcludedColumn)
      .collect(Collectors.joining(", "));
    return "ON CONFLICT(" + idColumnName + ") DO UPDATE SET " + updateColumns;
  }

  public static String limitQuery(int limit) {
    return "LIMIT " + limit;
  }

  private static String assignParameters(String[] columns) {
    return Arrays.stream(columns)
      .map(SqlQueryHelper::assignParameter)
      .collect(Collectors.joining(", "));
  }

  private static String assignParameter(String column) {
    return column + " = ?";
  }

  private static String assignExcludedColumn(String column) {
    return column + "= EXCLUDED." + column;
  }

  private static String joinWithComa(String[] columns) {
    return String.join(", ", columns);
  }
}
