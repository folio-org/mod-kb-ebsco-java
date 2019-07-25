package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.RestConstants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.google.common.io.Files;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.SendContext;

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
   *
   * @param configurationsFile configuration file, first config object must contain url config
   * @param wiremockUrl        wiremock url with port
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
   * Mocks wiremock server to return empty test RM API configuration from specified file,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   * @param wiremockUrl wiremock url with port
   */
  public static void mockEmptyConfiguration(String wiremockUrl) throws IOException, URISyntaxException {
    String emptyConfiguration = "responses/kb-ebsco/configuration/get-configuration-empty.json";
    mockConfiguration(emptyConfiguration, wiremockUrl);
  }

  /**
   * Mocks wiremock server to return default test RM API configuration from specified file,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   * @param wiremockUrl wiremock url with port
   */
  public static void mockDefaultConfiguration(String wiremockUrl) throws IOException, URISyntaxException {
    String configurationsFile = "responses/kb-ebsco/configuration/get-configuration.json";
    mockConfiguration(configurationsFile, wiremockUrl);
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

  public static void mockGet(StringValuePattern urlPattern, String responseFile) throws IOException, URISyntaxException {
    mockGetWithBody(urlPattern, readFile(responseFile));
  }

  public static void mockGetWithBody(StringValuePattern urlPattern, String body) {
    stubFor(get(new UrlPathPattern(urlPattern, (urlPattern instanceof RegexPattern)))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(body)));
  }

  public static void mockGet(StringValuePattern urlPattern, int status) {
    stubFor(get(new UrlPathPattern(urlPattern, (urlPattern instanceof RegexPattern)))
      .willReturn(new ResponseDefinitionBuilder().withStatus(status)));
  }

  public static void mockPost(StringValuePattern urlPattern, ContentPattern body, String response, int status) throws IOException, URISyntaxException {
    stubFor(post(new UrlPathPattern(urlPattern, (urlPattern instanceof RegexPattern)))
      .withRequestBody(body)
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(readFile(response))
        .withStatus(status)));
  }

  public static void mockPut(StringValuePattern urlPattern, ContentPattern content, int status) {
    stubFor(put(new UrlPathPattern(urlPattern, (urlPattern instanceof RegexPattern)))
      .withRequestBody(content)
      .willReturn(new ResponseDefinitionBuilder()
        .withStatus(status)));
  }

  public static void mockPut(StringValuePattern urlPattern, int status) {
    stubFor(put(new UrlPathPattern(urlPattern, (urlPattern instanceof RegexPattern)))
      .willReturn(new ResponseDefinitionBuilder()
        .withStatus(status)));
  }

  public static void mockResponseList(UrlPathPattern urlPattern, ResponseDefinitionBuilder... responses) {
    int scenarioStep = 0;
    String scenarioName = "Scenario -" + UUID.randomUUID().toString();
    for (ResponseDefinitionBuilder response : responses) {
      if (scenarioStep == 0) {
        stubFor(
          get(urlPattern)
            .inScenario(scenarioName)
            .willSetStateTo(String.valueOf(++scenarioStep))
            .willReturn(response));
      } else {
        stubFor(
          get(urlPattern)
            .inScenario(scenarioName)
            .whenScenarioStateIs(String.valueOf(scenarioStep))
            .willSetStateTo(String.valueOf(++scenarioStep))
            .willReturn(response));
      }
    }
  }

  public static void clearDataFromTable(Vertx vertx, String tableName) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + (PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + tableName),
      event -> future.complete(null));
    future.join();
  }

  public static Handler<SendContext> interceptAndContinue(String serviceAddress, String serviceMethodName,
                                                          Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
        messageContext.next();
      } else {
        messageContext.next();
      }
    };
  }

  public static Handler<SendContext> interceptAndStop(String serviceAddress, String serviceMethodName,
                                                      Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
      } else {
        messageContext.next();
      }
    };
  }

  private static boolean messageMatches(String serviceAddress, String serviceMethodName, Message message) {
    return serviceAddress.equals(message.address())
      && serviceMethodName.equals(message.headers().get("action"));
  }
}
