package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentials;
import org.folio.rest.jaxrs.resource.EholdingsUserKbCredential;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("java:S6813")
public class EholdingsKbCredentialsImpl implements EholdingsKbCredentials, EholdingsUserKbCredential {

  @Autowired
  @Qualifier("securedCredentialsService")
  private KbCredentialsService securedCredentialsService;
  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService nonSecuredCredentialsService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsKbCredentialsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentials(Map<String, String> okapiHeaders,
                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                        Context vertxContext) {
    securedCredentialsService.findAll(okapiHeaders)
      .thenAccept(kbCredentialsCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsResponse.respond200WithApplicationVndApiJson(kbCredentialsCollection))))
      .exceptionally(errorHandler.handle(asyncResultHandler));

  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsKbCredentials(String contentType, KbCredentialsPostRequest entity,
                                         Map<String, String> okapiHeaders,
                                         Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    securedCredentialsService.save(entity, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        PostEholdingsKbCredentialsResponse.respond201WithApplicationVndApiJson(kbCredentials))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsById(String id, Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler,
                                            Context vertxContext) {
    securedCredentialsService.findById(id, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsByIdResponse.respond200WithApplicationVndApiJson(kbCredentials))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void patchEholdingsKbCredentialsById(String id, String contentType, KbCredentialsPatchRequest entity,
                                              Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    securedCredentialsService.updatePartially(id, entity, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsByIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsById(String id, String contentType, KbCredentialsPutRequest entity,
                                            Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    securedCredentialsService.update(id, entity, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsByIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteEholdingsKbCredentialsById(String id, Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler,
                                               Context vertxContext) {
    securedCredentialsService.delete(id, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        DeleteEholdingsKbCredentialsByIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  public void getEholdingsKbCredentialsKeyById(String id, Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler,
                                               Context vertxContext) {
    nonSecuredCredentialsService.findKeyById(id, okapiHeaders)
      .thenAccept(kbCredentialsKey -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsKeyByIdResponse.respond200WithApplicationVndApiJson(kbCredentialsKey))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsUserKbCredential(Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler,
                                           Context vertxContext) {
    nonSecuredCredentialsService.findByUser(okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsUserKbCredentialResponse.respond200WithApplicationVndApiJson(kbCredentials))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

}
