package org.folio.rest.util;

import java.util.Optional;

import io.vertx.core.json.JsonObject;

import org.folio.rest.tools.utils.JwtUtils;

public final class TokenUtil {

  private static final String USER_ID_KEY = "user_id";
  private static final String USERNAME_KEY = "sub";

  private TokenUtil() {

  }

  /**
   * Fetch userId and username from x-okapi-token
   *
   * @param token x-okapi-token to get info from
   * @return {@link UserInfo} that contains userId and username
   */
  public static Optional<UserInfo> userInfoFromToken(String token) {
    try {
      String[] split = token.split("\\.");
      JsonObject j = new JsonObject(JwtUtils.getJson(split[1]));
      return Optional.of(new UserInfo(j.getString(USER_ID_KEY), j.getString(USERNAME_KEY)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
