package org.folio.repository.currencies;

import static org.folio.repository.SqlQueryHelper.orderByQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;

public class CurrenciesConstants {

  public static final String CURRENCIES_TABLE_NAME = "currencies";

  public static final String CODE_COLUMN = "code";
  public static final String DESCRIPTION_COLUMN = "description";

  public static final String SELECT_CURRENCIES;

  static {
    SELECT_CURRENCIES = selectQuery() + " " + orderByQuery(DESCRIPTION_COLUMN) + ";";
  }

  private CurrenciesConstants() {
  }
}
