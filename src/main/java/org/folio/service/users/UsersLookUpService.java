package org.folio.service.users;

import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.okapi.common.XOkapiHeaders;

/**
 * Retrieves user information from mod-users /users/{userId} endpoint.
 */
@Component
public class UsersLookUpService {

  private static final Logger LOG = LogManager.getLogger(UsersLookUpService.class);

  private static final String USERS_ENDPOINT_TEMPLATE = "/users/%s";

  private static final String AUTHORIZATION_FAIL_ERROR_MESSAGE = "Authorization failure";
  private static final String USER_NOT_FOUND_ERROR_MESSAGE = "User not found";
  private static final String CANNOT_GET_USER_DATA_ERROR_MESSAGE = "Cannot get user data: %s";
  private static final String USER_INFO_IS_NOT_COMPLETE_ERROR_MESSAGE = "User info is not complete";

  private final WebClient webClient;

  public UsersLookUpService(@Autowired Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  /**
   * Returns the user information for the userid specified in X-Okapi-Token
   *
   * @param okapiParams The okapi params for the current API call.
   * @return User information.
   */
  public CompletableFuture<User> lookUpUser(final OkapiParams okapiParams) {
    MultiMap headers = new HeadersMultiMap();
    headers.addAll(okapiParams.getHeaders());
    headers.add(HttpHeaders.ACCEPT, HttpHeaderValues.APPLICATION_JSON);

    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    String userId = headers.get(XOkapiHeaders.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      String usersPath = String.format(USERS_ENDPOINT_TEMPLATE, userId);
      webClient.getAbs(headers.get(XOkapiHeaders.URL) + usersPath)
        .putHeaders(headers)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
        .send(promise);
      return mapVertxFuture(promise.future().map(HttpResponse::body).map(this::mapUser));
    } else {
      return CompletableFuture.failedFuture(new NotAuthorizedException(XOkapiHeaders.USER_ID + " header is required"));
    }
  }

  private ErrorConverter errorConverter() {
    return ErrorConverter.createFullBody(result -> {
      HttpResponse<Buffer> response = result.response();
      if (response.statusCode() == 401 || response.statusCode() == 403) {
        LOG.error(AUTHORIZATION_FAIL_ERROR_MESSAGE);
        throw new NotAuthorizedException(AUTHORIZATION_FAIL_ERROR_MESSAGE);
      } else if (response.statusCode() == 404) {
        LOG.error(USER_NOT_FOUND_ERROR_MESSAGE);
        throw new NotFoundException(USER_NOT_FOUND_ERROR_MESSAGE);
      } else {
        String message = result.message();
        String msg = String.format(CANNOT_GET_USER_DATA_ERROR_MESSAGE, message);
        LOG.error(msg);
        throw new IllegalStateException(message);
      }
    });
  }

  private User mapUser(JsonObject user) {
    User.UserBuilder builder = User.builder();
    if (user.containsKey("username") && user.containsKey("personal")) {
      builder.id(user.getString("id"));
      builder.userName(user.getString("username"));

      JsonObject personalInfo = user.getJsonObject("personal");
      if (personalInfo != null) {
        builder.firstName(personalInfo.getString("firstName"));
        builder.middleName(personalInfo.getString("middleName"));
        builder.lastName(personalInfo.getString("lastName"));
      }
    } else {
      throw new BadRequestException(USER_INFO_IS_NOT_COMPLETE_ERROR_MESSAGE);
    }
    return builder.build();
  }
}
