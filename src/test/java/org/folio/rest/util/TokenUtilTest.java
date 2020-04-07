package org.folio.rest.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class TokenUtilTest {


  @Test
  public void testValidToken() {
    String validToken = "eyJhbGciOiJIUzI1NiJ9."
      + "eyJzdWIiOiJURVNUX1VTRVJfTkFNRSIsInVzZXJfaWQiOiJURVNUX1VTRVJfSUQiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImRpa3UifQ."
      + "xoJ_lJqjGmDUdIoHDdTdPtssQnV_xjN7I8QPBsbrxi4";

    Optional<UserInfo> actual = TokenUtil.userInfoFromToken(validToken);

    assertTrue(actual.isPresent());
    assertEquals("TEST_USER_ID", actual.get().getUserId());
    assertEquals("TEST_USER_NAME", actual.get().getUsername());
  }

  @Test
  public void testInvalidToken() {
    String invalidToken = "invalidToken";

    Optional<UserInfo> actual = TokenUtil.userInfoFromToken(invalidToken);

    assertFalse(actual.isPresent());
  }
}
