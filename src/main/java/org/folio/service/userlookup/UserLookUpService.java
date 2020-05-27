package org.folio.service.userlookup;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import static org.folio.rest.util.RestConstants.OKAPI_URL_HEADER;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
public class UserLookUpService {

  private static final Logger LOG = LoggerFactory.getLogger(UserLookUpService.class);

  private static final String AUTHORIZATION_FAIL_ERROR_MESSAGE = "Authorization failure";
  private static final String USER_NOT_FOUND_ERROR_MESSAGE = "User not found";
  private static final String CANNOT_GET_USER_DATA_ERROR_MESSAGE = "Cannot get user data: {}";
  private static final String USER_INFO_IS_NOT_COMPLETE_ERROR_MESSAGE = "User info is not complete";

  /**
   * Returns the user information for the userid specified in X-Okapi-Token
   *
   * @param okapiHeaders The headers for the current API call.
   * @return User information.
   */
  public CompletableFuture<UserLookUp> getUserInfo(final Map<String, String> okapiHeaders) {
    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();
    headers.addAll(okapiHeaders);

    String tenantId = TenantTool.calculateTenantId(headers.get(XOkapiHeaders.TENANT));
    Optional<UserInfo> userInfo = TokenUtils.userInfoFromToken(headers.get(XOkapiHeaders.TOKEN));

    CompletableFuture<UserLookUp> future = new CompletableFuture<>();
    if (!userInfo.isPresent()) {
      future.completeExceptionally(new NotAuthorizedException(AUTHORIZATION_FAIL_ERROR_MESSAGE));
      return future;
    }
    String userId = userInfo.get().getUserId();

    String okapiURL = headers.get(OKAPI_URL_HEADER);
    String url = "/users/" + userId;
    try {
      final HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);
      httpClient.request(url, okapiHeaders)
        .thenApply(response -> {
          try {
            if (Response.isSuccess(response.getCode())) {
              return mapUserInfo(response);
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

  private UserLookUp mapUserInfo(Response response) {
    UserLookUp.UserLookUpBuilder builder = UserLookUp.builder();
    JsonObject user = response.getBody();
    if (user.containsKey("username") && user.containsKey("personal")) {
      builder.userId(user.getString("id"));
      builder.username(user.getString("username"));

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
