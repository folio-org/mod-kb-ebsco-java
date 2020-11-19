package org.folio.service.uc.export;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class TitleExportModel {

  @CsvBindByName(column = "Title")
  @CsvBindByPosition(position = 0)
  private String title;

  @CsvBindByName(column = "Type")
  @CsvBindByPosition(position = 1)
  private String type;

  @CsvBindByName(column = "Cost")
  @CsvBindByPosition(position = 2)
  private double cost;

  @CsvBindByName(column = "Usage")
  @CsvBindByPosition(position = 3)
  private int usage;

  @CsvBindByName(column = "Cost per use")
  @CsvBindByPosition(position = 4)
  private double costPerUse;

  @CsvBindByName(column = "% of usage")
  @CsvBindByPosition(position = 5)
  private double percent;

  @CsvBindByName(column = "Year")
  @CsvBindByPosition(position = 6)
  private String year;


}
