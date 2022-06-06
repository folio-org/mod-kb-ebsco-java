package org.folio.repository;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.folio.common.ListUtils;

public final class SqlQueryHelper {

  private static final String FROM_KEYWORD = "FROM %s";

  private SqlQueryHelper() {}

  public static String selectQuery(String... columns) {
    if (columns.length == 0) {
      return "SELECT * " + FROM_KEYWORD + " t1";
    } else {
      return "SELECT " + joinWithComma(columns) + " " + FROM_KEYWORD + " t1";
    }
  }

  public static String insertQuery(String... columns) {
    return insertQuery(1, columns);
  }

  public static String insertQuery(int size, String... columns) {
    return "INSERT INTO %s (" + joinWithComma(columns) + ") VALUES "
      + ListUtils.createPlaceholders(columns.length, size);
  }

  public static String deleteQuery() {
    return "DELETE " + FROM_KEYWORD;
  }

  public static String leftJoinQuery(String targetTable, String columnT1, String columnT2) {
    return "LEFT JOIN " + targetTable + " t2 ON t1." + columnT1 + " = t2." + columnT2;
  }

  public static String count() {
    return count("*", "count");
  }

  public static String count(String expression, String asColumn) {
    return "COUNT(" + expression + ") as " + asColumn;
  }

  public static String groupByQuery(String... columns) {
    return "GROUP BY " + joinWithComma(columns);
  }

  public static String orderByQuery(String... columns) {
    return "ORDER BY " + joinWithComma(columns);
  }

  public static String whereQuery(String... columns) {
    String whereQuery = Arrays.stream(columns)
      .map(SqlQueryHelper::equalCondition)
      .collect(Collectors.joining(" AND "));
    return "WHERE " + whereQuery;
  }

  public static String whereConditionsQuery(String... conditions) {
    return "WHERE " + String.join(" AND ", conditions);
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

  public static String nothingOnConflictedIdQuery(String idColumnName) {
    return "ON CONFLICT(" + idColumnName + ") DO NOTHING";
  }

  public static String limitQuery() {
    return limit("?");
  }

  public static String limitQuery(int limit) {
    return limit(String.valueOf(limit));
  }

  public static String offsetQuery() {
    return offset("?");
  }

  public static String offsetQuery(int offset) {
    return offset(String.valueOf(offset));
  }

  public static String equalCondition(String column) {
    return column + " = ?";
  }

  public static String lessThanCondition(String column) {
    return column + " < ?";
  }

  public static String likeCondition(String column) {
    return column + " LIKE ?";
  }

  public static String inCondition(String column) {
    return inCondition(column, "%s");
  }

  public static String inCondition(String column, String query) {
    return column + " IN (" + query + ")";
  }

  private static String assignParameters(String[] columns) {
    return Arrays.stream(columns)
      .map(SqlQueryHelper::equalCondition)
      .collect(Collectors.joining(", "));
  }

  private static String assignExcludedColumn(CharSequence column) {
    return column + " = EXCLUDED." + column;
  }

  public static String joinWithComma(CharSequence... columns) {
    return String.join(", ", columns);
  }

  private static String offset(String offset) {
    return "OFFSET " + offset;
  }

  private static String limit(String limit) {
    return "LIMIT " + limit;
  }
}
