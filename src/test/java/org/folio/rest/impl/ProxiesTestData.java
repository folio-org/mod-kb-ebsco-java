package org.folio.rest.impl;

import io.restassured.http.Header;

import org.folio.okapi.common.XOkapiHeaders;

public class ProxiesTestData {

  public static final String STUB_CREDENTILS_ID = "12312312-1231-1231-a111-111111111111";

  public static final String JOHN_ID = "47d9ca93-9c82-4d6a-8d7f-7a73963086b9";
  public static final String johnToken = "eyJhbGciOiJIUzI1NiJ9." +
    "eyJzdWIiOiJqb2huX2RvZSIsInVzZXJfaWQiOiI0N2Q5Y2E5My05YzgyLTRkNmEtOGQ3Zi03YTczOTYzMDg2Y" +
    "jkiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImZzIn0.HTx-4aUFIPtEHO-6ZcYML6K3-0VRDGv3KX44JoT3hxg";
  public static final Header JOHN_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN, johnToken);

  public static final String JANE_ID = "781fce7d-5cf5-490d-ad89-a3d192eb526c";
  public static final String janeToken = "eyJhbGciOiJIUzI1NiJ9." +
    "eyJzdWIiOiJqYW5lX2RvZSIsInVzZXJfaWQiOiI3ODFmY2U3ZC01Y2Y1LTQ5MGQtYWQ4OS1hM2QxOTJlYjUyN" +
    "mMiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImZzIn0.kM0PYy49d92g5qhqPgTFz8aknjO7fQlZ5kljCC_M3-c";
  public static final Header JANE_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN, janeToken);

}
