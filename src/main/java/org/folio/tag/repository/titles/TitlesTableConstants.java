package org.folio.tag.repository.titles;

public class TitlesTableConstants {

  public static final String TITLES_TABLE_NAME = "titles";
  public static final String ID_COLUMN = "id";
  public static final String NAME_COLUMN = "name";
  public static final String TITLE_FIELD_LIST = String.format("%s, %s", ID_COLUMN, NAME_COLUMN);
  public static final String INSERT_OR_UPDATE_TITLE_STATEMENT = "INSERT INTO %s(" + TITLE_FIELD_LIST + ") VALUES (?, ?) "
    + "ON CONFLICT (" + ID_COLUMN + ") DO UPDATE " + "SET " + NAME_COLUMN + " = ?;";
  public static final String DELETE_TITLE_STATEMENT = "DELETE FROM %s " + "WHERE " + ID_COLUMN + "=?";

  private TitlesTableConstants() {}

}

