package org.folio.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.vertx.core.json.JsonObject;

public final class TokenTestUtils {

  private TokenTestUtils() {

  }

  public static String generateToken(String username, String userId) {
    JsonObject header = new JsonObject().put("alg", "HS256");
    JsonObject data = new JsonObject().put("sub", username).put("user_id", userId);
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
