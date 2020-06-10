package org.folio.rest.impl;


import static io.vertx.core.Future.succeededFuture;

import static org.folio.rest.util.ExceptionMappers.error401NotAuthorizedMapper;

import java.util.Map;

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

import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.resource.EholdingsLoadingKbCredentialsId;
import org.folio.rest.jaxrs.resource.LoadHoldings;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.ExceptionMappers;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.HoldingsStatusAuditService;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.spring.SpringContextUtil;

public class LoadHoldingsImpl implements LoadHoldings, EholdingsLoadingKbCredentialsId {

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


  public LoadHoldingsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postLoadHoldings(String contentType, Map<String, String> okapiHeaders,
                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Received signal to start scheduled loading of holdings");
    asyncResultHandler.handle(succeededFuture(Response.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsLoadingKbCredentialsStatusById(String id, String contentType, Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Getting holdings loading status");
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template
      .requestAction(context ->
        holdingsStatusRepository.findByCredentialsId(context.getCredentialsId(), context.getOkapiData().getTenant()))
      .addErrorMapper(NotAuthorizedException.class, error401NotAuthorizedMapper())
      .addErrorMapper(NotFoundException.class, ExceptionMappers.error404NotFoundMapper())
      .executeWithResult(HoldingsLoadingStatus.class);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsLoadingKbCredentialsById(String id, String contentType, Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Start loading of holdings for credentials: " + id);
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template.requestAction(context -> holdingsStatusAuditService.clearExpiredRecords(id, context.getOkapiData().getTenant())
      .thenCompose(o -> holdingsService.loadHoldingsById(id, context))
    )
      .addErrorMapper(ProcessInProgressException.class,
        e -> PostEholdingsLoadingKbCredentialsByIdResponse.respond409WithTextPlain(ErrorUtil.createError(e.getMessage())))
      .addErrorMapper(NotFoundException.class, ExceptionMappers.error404NotFoundMapper())
      .execute();
  }
}

