package org.folio.service.users;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import org.folio.common.OkapiParams;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.util.TokenUtils;
import org.folio.util.UserInfo;

/**
 * Retrieves user information from mod-users /users/{userId} endpoint.
 */
@Component
public class UsersLookUpService {

  private static final Logger LOG = LogManager.getLogger(UsersLookUpService.class);

  private static final String AUTHORIZATION_FAIL_ERROR_MESSAGE = "Authorization failure";
  private static final String USER_NOT_FOUND_ERROR_MESSAGE = "User not found";
  private static final String CANNOT_GET_USER_DATA_ERROR_MESSAGE = "Cannot get user data: {}";
  private static final String USER_INFO_IS_NOT_COMPLETE_ERROR_MESSAGE = "User info is not complete";

  /**
   * Returns the user information for the userid specified in X-Okapi-Token
   *
   * @param okapiParams The okapi params for the current API call.
   * @return User information.
   */
  public CompletableFuture<User> lookUpUser(final OkapiParams okapiParams) {
    Map<String, String> headers = okapiParams.getHeaders();

    String tenantId = TenantTool.calculateTenantId(headers.get(XOkapiHeaders.TENANT));
    Optional<UserInfo> userInfo = TokenUtils.userInfoFromToken(headers.get(XOkapiHeaders.TOKEN));

    CompletableFuture<User> future = new CompletableFuture<>();
    if (userInfo.isEmpty()) {
      future.completeExceptionally(new NotAuthorizedException(AUTHORIZATION_FAIL_ERROR_MESSAGE));
      return future;
    }
    String userId = userInfo.get().getUserId();

    String okapiURL = headers.get(XOkapiHeaders.URL);
    String url = "/users/" + userId;
    try {
      final HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);
      httpClient.request(url, headers)
        .thenApply(response -> {
          try {
            if (Response.isSuccess(response.getCode())) {
              return mapUser(response);
            } else if (response.getCode() == SC_UNAUTHORIZED || response.getCode() == SC_FORBIDDEN) {
              LOG.error(AUTHORIZATION_FAIL_ERROR_MESSAGE);
              throw new NotAuthorizedException(AUTHORIZATION_FAIL_ERROR_MESSAGE);
            } else if (response.getCode() == SC_NOT_FOUND) {
              LOG.error(USER_NOT_FOUND_ERROR_MESSAGE);
              throw new NotFoundException(USER_NOT_FOUND_ERROR_MESSAGE);
            } else {
              LOG.error(CANNOT_GET_USER_DATA_ERROR_MESSAGE, response.getError());
              throw new IllegalStateException(response.getError().toString());
            }
          } finally {
            httpClient.closeClient();
          }
        })
        .thenAccept(future::complete)
        .exceptionally(e -> {
          future.completeExceptionally(e.getCause());
          return null;
        });
    } catch (Exception e) {
      LOG.error(CANNOT_GET_USER_DATA_ERROR_MESSAGE, e.getMessage());
      future.completeExceptionally(e);
    }

    return future;
  }

  private User mapUser(Response response) {
    User.UserBuilder builder = User.builder();
    JsonObject user = response.getBody();
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
