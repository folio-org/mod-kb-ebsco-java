package org.folio.rest.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TokenUtilTest {


  @Test
  public void testValidToken() {
    String validToken = "eyJhbGciOiJIUzI1NiJ9."
      + "eyJzdWIiOiJURVNUX1VTRVJfTkFNRSIsInVzZXJfaWQiOiJURVNUX1VTRVJfSUQiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImRpa3UifQ."
      + "xoJ_lJqjGmDUdIoHDdTdPtssQnV_xjN7I8QPBsbrxi4";

    Optional<Pair<String, String>> actual = TokenUtil.userFromToken(validToken);

    assertTrue(actual.isPresent());
    assertEquals("TEST_USER_ID", actual.get().getKey());
    assertEquals("TEST_USER_NAME", actual.get().getValue());
  }

  @Test
  public void testInvalidToken() {
    String invalidToken = "invalidToken";

    Optional<Pair<String, String>> actual = TokenUtil.userFromToken(invalidToken);

    assertFalse(actual.isPresent());
  }
}
