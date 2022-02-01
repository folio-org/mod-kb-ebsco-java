package org.folio.repository.currencies;

import static org.folio.repository.DbUtil.getCurrenciesTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.orderByQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;

public class CurrenciesConstants {

  public static final String CURRENCIES_TABLE_NAME = "currencies";
  public static final String CODE_COLUMN = "code";
  public static final String DESCRIPTION_COLUMN = "description";

  private CurrenciesConstants() {
  }

  public static String selectCurrencies(String tenantId) {
    return prepareQuery(selectCurrencies(), getCurrenciesTableName(tenantId));
  }

  private static String selectCurrencies() {
    return selectQuery() + " " + orderByQuery(DESCRIPTION_COLUMN) + ";";
  }

}
