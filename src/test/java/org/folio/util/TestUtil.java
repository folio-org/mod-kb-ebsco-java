package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.google.common.io.Files;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.util.RestConstants;

public final class TestUtil {

  public static final String STUB_TENANT = "fs";
  public static final String STUB_TOKEN = "TEST_OKAPI_TOKEN";

  private TestUtil() {
  }

  /**
   * Reads file from classpath as String
   */
  public static String readFile(String filename) throws IOException, URISyntaxException {
    return Files.asCharSource(getFile(filename), StandardCharsets.UTF_8).read();
  }

  /**
   * Returns File object corresponding to the file on classpath with specified filename
   */
  public static File getFile(String filename) throws URISyntaxException {
    return new File(TestUtil.class.getClassLoader()
      .getResource(filename).toURI());
  }

  /**
   * Mocks wiremock server to return RM API configuration from specified file,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   * @param configurationsFile configuration file, first config object must contain url config
   * @param wiremockUrl wiremock url with port
   */
  public static void mockConfiguration(String configurationsFile, String wiremockUrl) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    Configs configurations = mapper.readValue(TestUtil.getFile(configurationsFile), Configs.class);
    if (!configurations.getConfigs().isEmpty()) {
      configurations.getConfigs().get(0).setValue(wiremockUrl);
    }

    stubFor(get(new UrlPathPattern(new EqualToPattern("/configurations/entries"), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(configurations))));
  }

  /**
   * Creates request specification with predefined common headers
   *
   * @param uri base uri
   * @return {@link RequestSpecBuilder}
   */
  public static RequestSpecBuilder getRequestSpecificationBuilder(String uri) {
    return new RequestSpecBuilder()
      .addHeader(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT)
      .addHeader(RestConstants.OKAPI_TOKEN_HEADER, STUB_TOKEN)
      .setBaseUri(uri)
      .log(LogDetail.ALL);
  }

  public static void mockGet(String requestUrlPattern, String responseFile) throws IOException, URISyntaxException {
    stubFor(get(new UrlPathPattern(new RegexPattern(requestUrlPattern), true))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(readFile(responseFile))));
  }

  public static void mockGet(String requestUrlPattern, int status) {
    stubFor(get(new UrlPathPattern(new RegexPattern(requestUrlPattern), true))
      .willReturn(new ResponseDefinitionBuilder().withStatus(status)));
  }
}
