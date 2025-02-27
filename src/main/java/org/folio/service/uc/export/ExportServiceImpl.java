package org.folio.service.uc.export;

import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;
import static com.opencsv.ICSVWriter.NO_ESCAPE_CHARACTER;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.converter.costperuse.export.PackageTitlesCostPerUseCollectionToExportConverter;
import org.folio.service.locale.LocaleSettingsService;
import org.folio.service.uc.UcCostPerUseService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ExportServiceImpl implements ExportService {

  private final UcCostPerUseService costPerUseService;
  private final LocaleSettingsService localeSettingsService;
  private final PackageTitlesCostPerUseCollectionToExportConverter converter;

  public ExportServiceImpl(UcCostPerUseService costPerUseService, LocaleSettingsService localeSettingsService,
                           PackageTitlesCostPerUseCollectionToExportConverter converter) {
    this.costPerUseService = costPerUseService;
    this.localeSettingsService = localeSettingsService;
    this.converter = converter;
  }

  public CompletableFuture<String> exportCsv(String packageId, String platform, String year,
                                             Map<String, String> okapiHeaders) {
    log.info("Perform export for package - {}", packageId);
    return costPerUseService.getPackageResourcesCostPerUse(packageId, platform, year, okapiHeaders)
      .thenCombine(localeSettingsService.retrieveSettings(new OkapiData(okapiHeaders)),
        (collection, localeSettings) -> converter.convert(collection, platform, year, localeSettings))
      .thenCompose(this::mapToCsv);
  }

  private CompletableFuture<String> mapToCsv(List<TitleExportModel> entities) {
    CompletableFuture<String> result = new CompletableFuture<>();
    log.info("Mapping {} entities to SCV", entities.size());
    StringWriter writer = new StringWriter();

    // mapping of columns by position
    CustomBeanToCsvMappingStrategy<TitleExportModel> mappingStrategy = new CustomBeanToCsvMappingStrategy<>();
    mappingStrategy.setType(TitleExportModel.class);

    StatefulBeanToCsv<TitleExportModel> csvToBean = new StatefulBeanToCsvBuilder<TitleExportModel>(writer)
      .withMappingStrategy(mappingStrategy)
      .withEscapechar(NO_ESCAPE_CHARACTER)
      .withLineEnd(DEFAULT_LINE_END)
      .withSeparator('|')
      .withApplyQuotesToAll(true)
      .build();

    try {
      csvToBean.write(entities);
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
      log.warn("Error occurred during mapping", e);
      result.completeExceptionally(new ExportException(e.getMessage()));
    }
    result.complete(writer.toString());
    return result;
  }
}
