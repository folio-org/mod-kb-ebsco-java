package org.folio.rest.util;

import java.util.Optional;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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
   * @return {@link Pair} of userId and username
   */
  public static Optional<Pair<String, String>> userFromToken(String token) {
    try {
      String[] split = token.split("\\.");
      JsonObject j = new JsonObject(JwtUtils.getJson(split[1]));
      return Optional.of(new ImmutablePair<>(j.getString(USER_ID_KEY), j.getString(USERNAME_KEY)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
