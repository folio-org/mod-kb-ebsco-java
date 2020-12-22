package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import static org.folio.rest.util.ExceptionMappers.error401NotAuthorizedMapper;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.ExceptionMappers.error422UcSettingsInvalidMapper;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdUc;
import org.folio.rest.jaxrs.resource.EholdingsUc;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.uc.UCSettingsService;
import org.folio.service.uc.UcAuthenticationException;
import org.folio.spring.SpringContextUtil;

public class EholdingsUsageConsolidationImpl implements EholdingsKbCredentialsIdUc, EholdingsUc {

  @Autowired
  @Qualifier("securedUCSettingsService")
  private UCSettingsService settingsService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsUsageConsolidationImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getEholdingsUc(boolean metricType, Map<String, String> okapiHeaders,
                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.fetchByUser(metricType, okapiHeaders)
      .thenAccept(ucSettings -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsUcResponse.respond200WithApplicationVndApiJson(ucSettings))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  public void getEholdingsKbCredentialsUcById(String credentialsId, boolean metricType, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.fetchByCredentialsId(credentialsId, metricType, okapiHeaders)
      .thenAccept(ucSettings -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsUcByIdResponse.respond200WithApplicationVndApiJson(ucSettings))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  public void postEholdingsKbCredentialsUcById(String id, String contentType, UCSettingsPostRequest entity,
                                               Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.save(id, entity, okapiHeaders)
      .thenAccept(ucSettings -> asyncResultHandler.handle(succeededFuture(
        PostEholdingsKbCredentialsUcByIdResponse.respond201WithApplicationVndApiJson(ucSettings))))
      .exceptionally(handleStatusException(asyncResultHandler));
  }

  @Override
  public void patchEholdingsKbCredentialsUcById(String credentialsId, UCSettingsPatchRequest entity,
                                                Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.update(credentialsId, entity, okapiHeaders)
      .thenAccept(unused -> asyncResultHandler.handle(succeededFuture(
        PatchEholdingsKbCredentialsUcByIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  private Function<Throwable, Void> handleStatusException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return e -> {
      ErrorHandler handler = new ErrorHandler();

      handler
        .add(InputValidationException.class, error422InputValidationMapper())
        .add(NotAuthorizedException.class, error401NotAuthorizedMapper())
        .add(UcAuthenticationException.class, error422UcSettingsInvalidMapper());

      handler.handle(asyncResultHandler, e);
      return null;
    };
  }
}
