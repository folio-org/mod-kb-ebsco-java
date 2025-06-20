package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.resource.EholdingsPackagesPackageIdResourcesCostperuseExport;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.uc.export.ExportService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("java:S6813")
public class EholdingsExportImpl implements EholdingsPackagesPackageIdResourcesCostperuseExport {

  @Autowired
  private ExportService exporterService;

  @Autowired
  private ErrorHandler exportErrorHandler;

  public EholdingsExportImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackagesResourcesCostperuseExportByPackageId(
    @Pattern(regexp = "^\\d+-\\d+$") String packageId,
    String platform,
    @Pattern(regexp = "^\\d{4}$") String fiscalYear,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    exporterService.exportCsv(packageId, platform, fiscalYear, okapiHeaders)
      .thenAccept(result ->
        asyncResultHandler.handle(Future.succeededFuture(
          EholdingsPackagesPackageIdResourcesCostperuseExport
            .GetEholdingsPackagesResourcesCostperuseExportByPackageIdResponse.respond200WithTextCsv(result))))
      .exceptionally(exportErrorHandler.handle(asyncResultHandler));
  }
}
