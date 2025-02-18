package org.folio.service.users;

import static java.util.Collections.emptyList;
import static org.folio.util.FutureUtils.mapVertxFuture;

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
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.folio.common.OkapiParams;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.util.RequestHeadersUtil;
import org.folio.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Retrieves user information from mod-users /users/{userId} endpoint.
 */
@Log4j2
@Component
public class UsersLookUpService {
  private static final String USERS_ENDPOINT_TEMPLATE = "/users/%s";
  private static final String USERS_ENDPOINT = "/users";
  private static final String GROUPS_ENDPOINT = "/groups";

  private static final String CQL_QUERY_PARAM = "query";
  private static final String LIMIT_PARAM = "limit";
  private static final String AUTHORIZATION_FAIL_ERROR_MESSAGE = "Authorization failure";
  private static final String USER_NOT_FOUND_ERROR_MESSAGE = "User not found";
  private static final String CANNOT_GET_USER_DATA_ERROR_MESSAGE = "Cannot get user data: {}. Server response: {}";
  private static final String USER_INFO_IS_NOT_COMPLETE_ERROR_MESSAGE = "User info is not complete";

  private final WebClient webClient;

  public UsersLookUpService(@Autowired Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  /**
   * Returns the user information for the userid specified in X-Okapi-Token.
   *
   * @param okapiParams The okapi params for the current API call.
   * @return User information.
   */
  public CompletableFuture<User> lookUpUser(final OkapiParams okapiParams) {
    MultiMap headers = new HeadersMultiMap();
    headers.addAll(okapiParams.getHeaders());
    headers.add(HttpHeaders.ACCEPT, HttpHeaderValues.APPLICATION_JSON);

    return RequestHeadersUtil.userIdFuture(okapiParams.getHeaders())
      .thenCompose(userId -> {
        Promise<HttpResponse<JsonObject>> promise = Promise.promise();
        sendUserRequest(userId, headers, promise);
        return mapVertxFuture(promise.future().map(HttpResponse::body).map(this::mapUser));
      });
  }

  public CompletableFuture<List<User>> lookUpUsers(List<UUID> ids, final OkapiParams okapiParams) {
    if (ids.isEmpty()) {
      return CompletableFuture.completedFuture(emptyList());
    }
    return lookUpUsersUsingCql(getIdsCql(ids), ids.size(), okapiParams);
  }

  public CompletableFuture<List<Group>> lookUpGroups(List<UUID> ids, final OkapiParams okapiParams) {
    if (ids.isEmpty()) {
      return CompletableFuture.completedFuture(emptyList());
    }
    return lookUpGroupsUsingCql(getIdsCql(ids), ids.size(), okapiParams);
  }

  public CompletableFuture<User> lookUpUserById(String userId, OkapiParams okapiParams) {
    MultiMap headers = new HeadersMultiMap();
    headers.addAll(okapiParams.getHeaders());
    headers.add(HttpHeaders.ACCEPT, HttpHeaderValues.APPLICATION_JSON);

    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    if (StringUtils.isNotBlank(userId)) {
      sendUserRequest(userId, headers, promise);
      return mapVertxFuture(promise.future().map(HttpResponse::body).map(this::mapUser));
    } else {
      return CompletableFuture.failedFuture(new NotAuthorizedException(XOkapiHeaders.USER_ID + " header is required"));
    }
  }

  private void sendUserRequest(String userId, MultiMap headers, Promise<HttpResponse<JsonObject>> promise) {
    String usersPath = String.format(USERS_ENDPOINT_TEMPLATE, userId);
    webClient.getAbs(headers.get(XOkapiHeaders.URL) + usersPath)
      .putHeaders(headers)
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .send(promise);
  }

  private CompletableFuture<List<User>> lookUpUsersUsingCql(String query, int limit, OkapiParams okapiParams) {
    Promise<HttpResponse<JsonObject>> promise = lookUpByCql(USERS_ENDPOINT, query, limit, okapiParams);
    return mapVertxFuture(promise.future().map(HttpResponse::body).map(this::mapUserCollection));
  }

  private CompletableFuture<List<Group>> lookUpGroupsUsingCql(String query, int limit, OkapiParams okapiParams) {
    Promise<HttpResponse<JsonObject>> promise = lookUpByCql(GROUPS_ENDPOINT, query, limit, okapiParams);
    return mapVertxFuture(promise.future().map(HttpResponse::body).map(this::mapGroupCollection));
  }

  private Promise<HttpResponse<JsonObject>> lookUpByCql(String path, String query, int limit, OkapiParams okapiParams) {
    MultiMap headers = new HeadersMultiMap();
    headers.addAll(okapiParams.getHeaders());
    headers.add(HttpHeaders.ACCEPT, HttpHeaderValues.APPLICATION_JSON);

    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    webClient.getAbs(headers.get(XOkapiHeaders.URL) + path)
      .putHeaders(headers)
      .addQueryParam(CQL_QUERY_PARAM, query)
      .addQueryParam(LIMIT_PARAM, String.valueOf(limit))
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .send(promise);
    return promise;
  }

  private ErrorConverter errorConverter() {
    return ErrorConverter.createFullBody(result -> {
      HttpResponse<Buffer> response = result.response();
      if (response.statusCode() == 401 || response.statusCode() == 403) {
        log.warn(AUTHORIZATION_FAIL_ERROR_MESSAGE);
        throw new NotAuthorizedException(AUTHORIZATION_FAIL_ERROR_MESSAGE);
      } else if (response.statusCode() == 404) {
        log.warn(USER_NOT_FOUND_ERROR_MESSAGE);
        throw new NotFoundException(USER_NOT_FOUND_ERROR_MESSAGE);
      } else {
        String message = result.message();
        String serverResponse = response.bodyAsString();
        log.warn(CANNOT_GET_USER_DATA_ERROR_MESSAGE, message, serverResponse);
        throw new IllegalStateException(message);
      }
    });
  }

  private List<User> mapUserCollection(JsonObject userCollection) {
    List<User> collection = new ArrayList<>();
    var users = userCollection.getJsonArray("users");
    users.stream().forEach(user -> collection.add(mapUser((JsonObject) user)));
    return collection;
  }

  private User mapUser(JsonObject user) {
    User.UserBuilder builder = User.builder();
    builder.id(user.getString("id"));
    builder.userName(user.getString("username"));
    builder.patronGroup(user.getString("patronGroup"));
    JsonObject personalInfo = user.getJsonObject("personal");
    if (personalInfo != null) {
      builder.firstName(personalInfo.getString("firstName"));
      builder.middleName(personalInfo.getString("middleName"));
      builder.lastName(personalInfo.getString("lastName"));
    }
    return builder.build();
  }

  private List<Group> mapGroupCollection(JsonObject groupCollection) {
    List<Group> collection = new ArrayList<>();
    var groups = groupCollection.getJsonArray("usergroups");
    groups.stream().forEach(group -> collection.add(mapGroup((JsonObject) group)));
    return collection;
  }

  private Group mapGroup(JsonObject group) {
    if (group.containsKey("group")) {
      return Group.builder()
        .id(group.getString("id"))
        .group(group.getString("group"))
        .build();
    } else {
      throw new BadRequestException(USER_INFO_IS_NOT_COMPLETE_ERROR_MESSAGE);
    }
  }

  @NotNull
  private String getIdsCql(List<UUID> ids) {
    var idsQueryValue = ids.stream()
      .map(UUID::toString)
      .map(StringUtil::cqlEncode)
      .collect(Collectors.joining(" OR "));
    return "id=(" + idsQueryValue + ")";
  }
}
