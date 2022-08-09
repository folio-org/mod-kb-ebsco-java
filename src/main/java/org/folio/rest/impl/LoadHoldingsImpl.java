package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.TenantUtil.tenantId;
import static org.folio.util.FutureUtils.failedFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.resource.EholdingsLoadingKbCredentials;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.util.template.RmApiTemplateFactory;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.HoldingsStatusAuditService;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class LoadHoldingsImpl implements EholdingsLoadingKbCredentials {

  private static final Logger LOG = LogManager.getLogger(LoadHoldingsImpl.class);

  private static final String LOADING_IN_PROGRESS_MESSAGE = "Holdings loading is already in progress";

  @Autowired
  private RmApiTemplateFactory templateFactory;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private HoldingsStatusAuditService holdingsStatusAuditService;
  @Autowired
  private HoldingsStatusRepository holdingsStatusRepository;
  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService credentialsService;
  @Autowired
  private ErrorHandler loadHoldingsErrorHandler;

  public LoadHoldingsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsLoadingKbCredentials(String contentType, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler,
                                                Context vertxContext) {
    LOG.info("Received signal to start scheduled loading of holdings");
    validateAllCredentialsStatus(okapiHeaders)
      .thenCompose(o -> credentialsService.findAll(okapiHeaders))
      .thenApply(KbCredentialsCollection::getData)
      .thenApply(credentialsList -> iterateOverCredentials(credentialsList, okapiHeaders))
      .thenAccept(anyObj -> asyncResultHandler.handle(
        succeededFuture(PostEholdingsLoadingKbCredentialsResponse.respond204())))
      .exceptionally(loadHoldingsErrorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsLoadingKbCredentialsById(String id, String contentType, Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    LOG.info("Start loading of holdings for credentials: " + id);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .withErrorHandler(loadHoldingsErrorHandler)
      .requestAction(this::validateAndStartLoading)
      .execute();
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsLoadingKbCredentialsStatusById(String id, String contentType,
                                                         Map<String, String> okapiHeaders,
                                                         Handler<AsyncResult<Response>> asyncResultHandler,
                                                         Context vertxContext) {
    LOG.info("Getting holdings loading status");
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .withErrorHandler(loadHoldingsErrorHandler)
      .requestAction(context ->
        holdingsStatusRepository.findByCredentialsId(toUUID(context.getCredentialsId()),
          context.getOkapiData().getTenant()))
      .executeWithResult(HoldingsLoadingStatus.class);
  }

  private CompletableFuture<Void> validateAndStartLoading(RmApiTemplateContext context) {
    return holdingsService.canStartLoading(context.getCredentialsId(), context.getOkapiData().getTenant())
      .thenCompose(canStartLoading -> canStartLoading
                                      ? startLoading(context)
                                      : failedFuture(new ProcessInProgressException(LOADING_IN_PROGRESS_MESSAGE)));
  }

  private CompletableFuture<Void> validateAllCredentialsStatus(Map<String, String> okapiHeaders) {
    return holdingsService.canStartLoading(tenantId(okapiHeaders))
      .thenCompose(allCanStartLoading -> allCanStartLoading
                                         ? CompletableFuture.completedFuture(null)
                                         : failedFuture(new ProcessInProgressException(LOADING_IN_PROGRESS_MESSAGE)));
  }

  private CompletableFuture<Void> iterateOverCredentials(List<KbCredentials> credentialsList,
                                                         Map<String, String> okapiHeaders) {
    return CompletableFuture.allOf(
      credentialsList.stream()
        .map(credentials -> buildContextAndRun(credentials, okapiHeaders))
        .toArray(CompletableFuture[]::new)
    );
  }

  private CompletableFuture<Void> buildContextAndRun(KbCredentials credentials, Map<String, String> okapiHeaders) {
    return startLoading(buildLoadingContext(credentials, okapiHeaders));
  }

  private CompletableFuture<Void> startLoading(RmApiTemplateContext context) {
    return holdingsStatusAuditService.clearExpiredRecords(context.getCredentialsId(),
        context.getOkapiData().getTenant())
      .thenCompose(o -> holdingsService.loadSingleHoldings(context));
  }

  private RmApiTemplateContext buildLoadingContext(KbCredentials credentials, Map<String, String> okapiHeaders) {
    return templateFactory.lookupContextBuilder()
      .okapiData(new OkapiData(okapiHeaders))
      .kbCredentials(credentials)
      .build();
  }

}

