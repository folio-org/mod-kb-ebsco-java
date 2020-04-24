package org.folio.rest.util;

import static org.folio.common.FutureUtils.failedFuture;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.NotAuthorizedException;

import io.vertx.core.json.JsonObject;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.tools.utils.JwtUtils;

public final class TokenUtil {

  private static final String INVALID_TOKEN_MESSAGE = "Invalid token";

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

  public static CompletableFuture<UserInfo> fetchUserInfo(Map<String, String> okapiHeaders) {
    return TokenUtil.userInfoFromToken(okapiHeaders.get(XOkapiHeaders.TOKEN))
      .map(CompletableFuture::completedFuture)
      .orElse(failedFuture(new NotAuthorizedException(INVALID_TOKEN_MESSAGE)));
  }

  public static String generateToken(String username, String userId) {
    JsonObject header = new JsonObject().put("alg", "HS256");
    JsonObject data = new JsonObject().put(USERNAME_KEY, username).put(USER_ID_KEY, userId);
    byte[] encodedHeader = Base64.getUrlEncoder().encode(header.toString().getBytes());
    byte[] encodedData = Base64.getUrlEncoder().encode(data.toString().getBytes());

    try {
      String message = new String(encodedHeader) + "." + new String(encodedData);
      Mac sha256HMAC = Mac.getInstance("HmacSHA256");

      SecretKeySpec secretKey = new SecretKeySpec("no-secret".getBytes(), "HmacSHA256");
      sha256HMAC.init(secretKey);

      byte[] signature = Base64.getUrlEncoder().encode(sha256HMAC.doFinal(message.getBytes()));
      return new String(encodedHeader) + "." + new String(encodedData) + "." + new String(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new UnsupportedOperationException(e);
    }
  }
}
