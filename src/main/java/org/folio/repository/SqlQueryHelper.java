package org.folio.repository;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.folio.common.ListUtils;

public final class SqlQueryHelper {

  private SqlQueryHelper() {

  }

  public static String selectQuery(String... columns) {
    if (columns.length == 0) {
      return "SELECT * FROM %s";
    } else {
      return "SELECT " + String.join(", ", columns) + " FROM %s";
    }
  }

  public static String insertQuery(String... columns) {
    return "INSERT INTO %s (" + String.join(", ", columns) + ") VALUES "
      + ListUtils.createInsertPlaceholders(columns.length, 1);
  }

  public static String whereQuery(String... columns) {
    String whereQuery = Arrays.stream(columns)
      .map(SqlQueryHelper::assignParameter)
      .collect(Collectors.joining(" AND "));
    return "WHERE " + whereQuery;
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

  private static String assignParameter(String column) {
    return column + " = ?";
  }

  private static String assignExcludedColumn(String column) {
    return column + "= EXCLUDED." + column;
  }
}
