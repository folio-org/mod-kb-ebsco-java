package org.folio.service.userlookup;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.folio.rest.RestVerticle;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

@Component
public class UserLookUpService {

  private static final Logger logger = LoggerFactory.getLogger(UserLookUpService.class);
  /**
   * Returns the user information for the userid specified in the original
   * request.
   *
   * @param okapiHeaders The headers for the current API call.
   * @return User information based on userid from header.
   */
  public CompletableFuture<UserLookUp> getUserInfo(final Map<String, String> okapiHeaders) {
    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();
    headers.addAll(okapiHeaders);

    final String tenantId = TenantTool.calculateTenantId(headers.get(RestVerticle.OKAPI_HEADER_TENANT));
    final String userId = headers.get(RestVerticle.OKAPI_USERID_HEADER);
    CompletableFuture<UserLookUp> future = new CompletableFuture();
    if (userId == null) {
      logger.error("No userid header");
      future.completeExceptionally(new BadRequestException("Missing user id header, cannot look up user"));
      return future;
    }

    String okapiURL = headers.get("X-Okapi-Url");
    String url = "/users/" + userId;
    try {
      final HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);
      httpClient.request(url, okapiHeaders)
        .thenApply(response -> {
          try {
            if (Response.isSuccess(response.getCode())) {
              return mapUserInfo(response);
            } else if (response.getCode() == 401 || response.getCode() == 403) {
              logger.error("Authorization failure");
              throw new NotAuthorizedException("Authorization failure");
            } else if (response.getCode() == 404) {
              logger.error("User not found");
              throw new NotFoundException("User not found");
            } else {
              logger.error("Cannot get user data: " + response.getError().toString(), response.getException());
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
      logger.error("Cannot get user data: " + e.getMessage(), e);
      future.completeExceptionally(e);
    }

    return future;
  }

  private UserLookUp mapUserInfo(Response response) {
    UserLookUp.UserLookUpBuilder builder = UserLookUp.builder();
    JsonObject user = response.getBody();
    if (user.containsKey("username") && user.containsKey("personal")) {
      builder.userName(user.getString("username"));

      JsonObject personalInfo = user.getJsonObject("personal");
      if (personalInfo != null) {
        builder.firstName(personalInfo.getString("firstName"));
        builder.middleName(personalInfo.getString("middleName"));
        builder.lastName(personalInfo.getString("lastName"));
      }
    } else {
      throw new BadRequestException("Missing fields");
    }
    return builder.build();
  }
}
