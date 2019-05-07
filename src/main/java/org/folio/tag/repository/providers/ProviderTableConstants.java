package org.folio.tag.repository.providers;

public class ProviderTableConstants {

  private ProviderTableConstants() {}

  public static final String PROVIDERS_TABLE_NAME = "providers";
  public static final String ID_COLUMN = "id";
  public static final String NAME_COLUMN = "name";
  public static final String PROVIDER_FIELD_LIST = String.format("%s, %s", ID_COLUMN, NAME_COLUMN);

  public static final String INSERT_OR_UPDATE_PROVIDER_STATEMENT = "INSERT INTO %s(" + PROVIDER_FIELD_LIST + ") VALUES (?, ?) "
      + "ON CONFLICT (" + ID_COLUMN + ") DO UPDATE " + "SET " + NAME_COLUMN + " = ?;";

  public static final String DELETE_PROVIDER_STATEMENT = "DELETE FROM %s " + "WHERE " + ID_COLUMN + "=?";
}
