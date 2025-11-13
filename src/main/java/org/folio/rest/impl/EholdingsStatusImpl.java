package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.EholdingsStatus.GetEholdingsStatusResponse.respond200WithApplicationVndApiJson;
import static org.folio.rest.util.ExceptionMappers.error400BadRequestMapper;
import static org.folio.rest.util.ExceptionMappers.error401NotAuthorizedMapper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.holdingsiq.model.ConfigurationError;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.configuration.StatusConverter;
import org.folio.rest.jaxrs.resource.EholdingsStatus;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.validator.HeaderValidator;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("java:S6813")
public class EholdingsStatusImpl implements EholdingsStatus {

  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private StatusConverter converter;

  public EholdingsStatusImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsStatus(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                 Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    MutableObject<OkapiData> okapiData = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        okapiData.setValue(new OkapiData(okapiHeaders));
        return configurationService.retrieveConfiguration(okapiData.get());
      })
      .thenCompose(configuration -> configurationService.verifyCredentials(configuration, vertxContext,
        okapiData.get())
      )
      .thenAccept(verificationErrorsToResponse(asyncResultHandler))
      .exceptionally(handleStatusException(asyncResultHandler));
  }

  private Consumer<List<ConfigurationError>> verificationErrorsToResponse(
    Handler<AsyncResult<Response>> asyncResultHandler) {
    return errors -> asyncResultHandler.handle(succeededFuture(respond200WithApplicationVndApiJson(converter.convert(
      errors.isEmpty()))));
  }

  private Function<Throwable, Void> handleStatusException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return e -> {
      ErrorHandler errorHandler = new ErrorHandler();

      errorHandler
        .addRmApiMapping()
        .add(NotFoundException.class, notFoundToInvalidStatusMapper())
        .add(BadRequestException.class, error400BadRequestMapper())
        .add(NotAuthorizedException.class, error401NotAuthorizedMapper());

      errorHandler.handle(asyncResultHandler, e);
      return null;
    };
  }

  private Function<NotFoundException, Response> notFoundToInvalidStatusMapper() {
    return e -> respond200WithApplicationVndApiJson(converter.convert(Boolean.FALSE));
  }
}
