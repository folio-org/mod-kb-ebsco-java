package org.folio.service.uc.export;

import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;
import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static com.opencsv.ICSVWriter.NO_ESCAPE_CHARACTER;
import static com.opencsv.ICSVWriter.NO_QUOTE_CHARACTER;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.service.uc.UCCostPerUseService;

@Service
public class ExportServiceImpl implements ExportService {
  private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

  @Autowired
  private UCCostPerUseService costPerUseService;
  @Autowired
  private Converter<ResourceCostPerUseCollection, List<TitleExportModel>> converter;

  public CompletableFuture<String> exportCSV(String packageId, String platform, String year,
                                             Map<String, String> okapiHeaders) {
    LOG.info("PACKAGE - " + packageId);
    return costPerUseService.getPackageResourcesCostPerUse(packageId, platform, year, okapiHeaders)
        .thenApply(converter::convert)
        .thenCompose(this::mapToCSV);
  }

  private CompletableFuture<String> mapToCSV(List<TitleExportModel> entities) {
    LOG.info("Start MAPPING TO SCV" + entities.size());
    StringWriter writer = new StringWriter();

    // mapping of columns by position
    CustomBeanToCSVMappingStrategy<TitleExportModel> mappingStrategy = new CustomBeanToCSVMappingStrategy<>();
    mappingStrategy.setType(TitleExportModel.class);

    StatefulBeanToCsv<TitleExportModel> csvToBean = new StatefulBeanToCsvBuilder<TitleExportModel>(writer)
      .withMappingStrategy(mappingStrategy)
      .withQuotechar(NO_QUOTE_CHARACTER)
      .withEscapechar(NO_ESCAPE_CHARACTER)
      .withLineEnd(DEFAULT_LINE_END)
//      .withSeparator('\t')
      .withSeparator('|')
      .build();

    try {
      if(CollectionUtils.isEmpty(entities)){
        csvToBean.write(new TitleExportModel());
      } else {
        csvToBean.write(entities);
      }
    } catch (CsvDataTypeMismatchException e) {
      e.printStackTrace();
    } catch (CsvRequiredFieldEmptyException e) {
      e.printStackTrace();
    }

    String s = writer.toString();
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    LOG.info("Finished MAPPING TO SCV: " + bytes.length + " bytes");
    return CompletableFuture.completedFuture(s);
  }

  public CompletableFuture<String> v2(List<TitleExportModel> entities) {
    StringWriter writer = new StringWriter();
    CSVWriter csvWriter;
    try {
      csvWriter = new CSVWriter(writer, DEFAULT_SEPARATOR, NO_QUOTE_CHARACTER, NO_ESCAPE_CHARACTER, DEFAULT_LINE_END);

      List<String[]> records = new ArrayList<>(entities.size());

      for (TitleExportModel model : entities)
      {
        List<String> record = Collections.singletonList(model.toString());

        String[] recordArray = new String[record.size()];
        record.toArray(recordArray);
        records.add(recordArray);
      }
      csvWriter.writeAll(records);
    }

    catch (Exception e) {
      e.printStackTrace();
    }
    return CompletableFuture.completedFuture(writer.toString());
  }
}
