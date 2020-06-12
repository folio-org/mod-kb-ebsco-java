package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.rest.util.ExceptionMappers.error401NotAuthorizedMapper;
import static org.folio.util.FutureUtils.failedFuture;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.rest.jaxrs.resource.EholdingsLoadingKbCredentials;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.ExceptionMappers;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateContextBuilder;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.HoldingsStatusAuditService;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.spring.SpringContextUtil;

public class LoadHoldingsImpl implements EholdingsLoadingKbCredentials {

  private static final Logger logger = LoggerFactory.getLogger(LoadHoldingsImpl.class);
  @Autowired
  private RMAPITemplateFactory templateFactory;
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
  private RMAPITemplateContextBuilder contextBuilder;
  @Autowired
  private ErrorHandler loadHoldingsErrorHandler;

  public LoadHoldingsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postEholdingsLoadingKbCredentials(String contentType, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Received signal to start scheduled loading of holdings");
    validateStatus(okapiHeaders)
      .thenCompose(o -> credentialsService.findAll(okapiHeaders)
        .thenAccept(credentialsList -> iterateOverCredentials(okapiHeaders, credentialsList)))
      .thenAccept(anyObj -> asyncResultHandler.handle(
        succeededFuture(PostEholdingsLoadingKbCredentialsResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsLoadingKbCredentialsStatusById(String id, String contentType, Map<String, String> okapiHeaders,
                                                         Handler<AsyncResult<Response>> asyncResultHandler,
                                                         Context vertxContext) {
    logger.info("Getting holdings loading status");
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template
      .requestAction(context ->
        holdingsStatusRepository.findByCredentialsId(toUUID(context.getCredentialsId()), context.getOkapiData().getTenant()))
      .addErrorMapper(NotAuthorizedException.class, error401NotAuthorizedMapper())
      .addErrorMapper(NotFoundException.class, ExceptionMappers.error404NotFoundMapper())
      .executeWithResult(HoldingsLoadingStatus.class);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsLoadingKbCredentialsById(String id, String contentType, Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    logger.info("Start loading of holdings for credentials: " + id);
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template.requestAction(this::startLoading)
      .addErrorMapper(ProcessInProgressException.class,
        e -> PostEholdingsLoadingKbCredentialsByIdResponse.respond409WithTextPlain(ErrorUtil.createError(e.getMessage())))
      .addErrorMapper(NotFoundException.class, ExceptionMappers.error404NotFoundMapper())
      .execute();
  }

  private CompletableFuture<Void> validateStatus(Map<String, String> okapiHeaders) {
    return holdingsStatusRepository.findAll(tenantId(okapiHeaders))
      .thenApply(statuses -> statuses.stream()
        .anyMatch(status -> LoadStatusNameEnum.IN_PROGRESS.equals(status.getData().getAttributes().getStatus().getName())))
      .thenCompose(isExists -> {
        if (Boolean.TRUE.equals(isExists)) {
          return failedFuture(
            new ProcessInProgressException("Loading status is already In Progress for one of the credentials"));
        }
        return CompletableFuture.completedFuture(null);
      });
  }

  private CompletableFuture<Void> iterateOverCredentials(Map<String, String> okapiHeaders,
                                                         KbCredentialsCollection credentialsList) {
    final List<KbCredentials> kbCredentials = credentialsList.getData();
    kbCredentials.forEach(credentials -> buldContextAndRun(okapiHeaders, credentials));
    return CompletableFuture.completedFuture(null);
  }

  private void buldContextAndRun(Map<String, String> okapiHeaders, KbCredentials credentials) {
    final RMAPITemplateContext context = buildLoadingContext(credentials, okapiHeaders);
    getStatus(context).thenAccept(status -> {
      final LoadStatusNameEnum name = status.getData().getAttributes().getStatus().getName();
      logger.info("Current status for credentials - {} is {}", credentials.getId(), name.value());
      if (LoadStatusNameEnum.NOT_STARTED.equals(name) || LoadStatusNameEnum.FAILED.equals(name)) {
        startLoading(context);
      }
    });
  }

  private CompletableFuture<HoldingsLoadingStatus> getStatus(RMAPITemplateContext context) {
    return holdingsStatusRepository
      .findByCredentialsId(toUUID(context.getCredentialsId()), context.getOkapiData().getTenant());
  }

  private CompletableFuture<Void> startLoading(RMAPITemplateContext context) {
    return holdingsStatusAuditService.clearExpiredRecords(context.getCredentialsId(), context.getOkapiData().getTenant())
      .thenCompose(o -> holdingsService.loadSingleHoldings(context));
  }

  private RMAPITemplateContext buildLoadingContext(KbCredentials credentials, Map<String, String> okapiHeaders) {
    return contextBuilder
      .okapiData(new OkapiData(okapiHeaders))
      .kbCredentials(credentials)
      .build();
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      loadHoldingsErrorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}

