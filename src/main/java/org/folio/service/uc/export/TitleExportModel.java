package org.folio.service.uc.export;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Builder;

@Builder
public class TitleExportModel {

  @CsvBindByName(column = "Title")
  @CsvBindByPosition(position = 0)
  private final String title;

  @CsvBindByName(column = "Type")
  @CsvBindByPosition(position = 1)
  private final String type;

  @CsvBindByName(column = "Usage")
  @CsvBindByPosition(position = 2)
  private final int usage;

  @CsvBindByName(column = "Cost")
  @CsvBindByPosition(position = 3)
  private final double cost;

  @CsvBindByName(column = "Cost_per_use")
  @CsvBindByPosition(position = 4)
  private final double costPerUse;

  @CsvBindByName(column = "Currency_selected")
  @CsvBindByPosition(position = 5)
  private final String currency;

  @CsvBindByName(column = "Percentage_of_usage")
  @CsvBindByPosition(position = 6)
  private final double percent;

  @CsvBindByName(column = "Year_selection")
  @CsvBindByPosition(position = 7)
  private final String year;

  @CsvBindByName(column = "Platform")
  @CsvBindByPosition(position = 8)
  private final String platform;

}
