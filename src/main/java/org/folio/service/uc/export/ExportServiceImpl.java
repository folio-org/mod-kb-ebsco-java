package org.folio.service.uc.export;

import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;
import static com.opencsv.ICSVWriter.NO_ESCAPE_CHARACTER;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.converter.costperuse.export.PackageTitlesCostPerUseCollectionToExportConverter;
import org.folio.service.locale.LocaleSettingsService;
import org.folio.service.uc.UCCostPerUseService;

@Service
public class ExportServiceImpl implements ExportService {
  private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

  @Autowired
  private UCCostPerUseService costPerUseService;
  @Autowired
  private LocaleSettingsService localeSettingsService;
  @Autowired
  private PackageTitlesCostPerUseCollectionToExportConverter converter;

  public CompletableFuture<String> exportCSV(String packageId, String platform, String year,
                                             Map<String, String> okapiHeaders) {
    LOG.info("Perform export for package - " + packageId);
    return costPerUseService.getPackageResourcesCostPerUse(packageId, platform, year, okapiHeaders)
      .thenCombine(localeSettingsService.retrieveSettings(new OkapiData(okapiHeaders)),
        ((collection, localeSettings) -> converter.convert(collection, platform, year, localeSettings))
      )
      .thenCompose(this::mapToCSV);
  }

  private CompletableFuture<String> mapToCSV(List<TitleExportModel> entities) {
    CompletableFuture<String> result = new CompletableFuture<>();
    LOG.info("Mapping " + entities.size() + " entities to SCV");
    StringWriter writer = new StringWriter();

    // mapping of columns by position
    CustomBeanToCSVMappingStrategy<TitleExportModel> mappingStrategy = new CustomBeanToCSVMappingStrategy<>();
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
      LOG.error("Error occurred during mapping", e);
      result.completeExceptionally(new ExportException(e.getMessage()));
    }
    result.complete(writer.toString());
    return result;
  }
}
