package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.RequestHeadersUtil.tenantId;
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
import org.folio.holdingsiq.model.RequestContext;
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

@SuppressWarnings("java:S6813")
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
  public void postEholdingsLoadingKbCredentials(String contentType, Map<String, String> headers,
                                                Handler<AsyncResult<Response>> asyncResultHandler,
                                                Context vertxContext) {
    LOG.info("Received signal to start scheduled loading of holdings");
    validateAllCredentialsStatus(headers)
      .thenCompose(o -> credentialsService.findAll(headers))
      .thenApply(KbCredentialsCollection::getData)
      .thenApply(credentialsList -> iterateOverCredentials(credentialsList, headers))
      .thenAccept(anyObj -> asyncResultHandler.handle(
        succeededFuture(PostEholdingsLoadingKbCredentialsResponse.respond204())))
      .exceptionally(loadHoldingsErrorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsLoadingKbCredentialsById(String id, String contentType, Map<String, String> headers,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    LOG.info("Start loading of holdings for credentials: {}", id);
    templateFactory.createTemplate(headers, asyncResultHandler)
      .withErrorHandler(loadHoldingsErrorHandler)
      .requestAction(this::validateAndStartLoading)
      .execute();
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsLoadingKbCredentialsStatusById(String id, String contentType,
                                                         Map<String, String> headers,
                                                         Handler<AsyncResult<Response>> asyncResultHandler,
                                                         Context vertxContext) {
    LOG.info("Getting holdings loading status");
    templateFactory.createTemplate(headers, asyncResultHandler)
      .withErrorHandler(loadHoldingsErrorHandler)
      .requestAction(context ->
        holdingsStatusRepository.findByCredentialsId(toUUID(context.getCredentialsId()),
          context.getRequestContext().getTenant()))
      .executeWithResult(HoldingsLoadingStatus.class);
  }

  private CompletableFuture<Void> validateAndStartLoading(RmApiTemplateContext context) {
    return holdingsService.canStartLoading(context.getCredentialsId(), context.getRequestContext().getTenant())
      .thenCompose(canStartLoading -> Boolean.TRUE.equals(canStartLoading)
                                      ? startLoading(context)
                                      : failedFuture(new ProcessInProgressException(LOADING_IN_PROGRESS_MESSAGE)));
  }

  private CompletableFuture<Void> validateAllCredentialsStatus(Map<String, String> headers) {
    return holdingsService.canStartLoading(tenantId(headers))
      .thenCompose(allCanStartLoading -> Boolean.TRUE.equals(allCanStartLoading)
                                         ? CompletableFuture.completedFuture(null)
                                         : failedFuture(new ProcessInProgressException(LOADING_IN_PROGRESS_MESSAGE)));
  }

  private CompletableFuture<Void> iterateOverCredentials(List<KbCredentials> credentialsList,
                                                         Map<String, String> headers) {
    return CompletableFuture.allOf(
      credentialsList.stream()
        .map(credentials -> buildContextAndRun(credentials, headers))
        .toArray(CompletableFuture[]::new)
    );
  }

  private CompletableFuture<Void> buildContextAndRun(KbCredentials credentials, Map<String, String> headers) {
    return startLoading(buildLoadingContext(credentials, headers));
  }

  private CompletableFuture<Void> startLoading(RmApiTemplateContext context) {
    return holdingsStatusAuditService.clearExpiredRecords(context.getCredentialsId(),
        context.getRequestContext().getTenant())
      .thenCompose(o -> holdingsService.loadSingleHoldings(context));
  }

  private RmApiTemplateContext buildLoadingContext(KbCredentials credentials, Map<String, String> headers) {
    return templateFactory.lookupContextBuilder()
      .requestContext(new RequestContext(headers))
      .kbCredentials(credentials)
      .build();
  }
}

