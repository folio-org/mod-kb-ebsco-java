package org.folio.util;

import static org.folio.repository.DbUtil.getTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.logger;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_KEY;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_CUSTOMER_ID;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentialsNonSecured;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.Message;

import org.folio.repository.SqlQueryHelper;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.holdings.message.LoadHoldingsMessage;

/**
 * Contains common methods that are used in mod-kb-ebsco-java
 */
public final class KBTestUtil {

  private KBTestUtil() {}

  /**
   * Mocks wiremock server to return default test RM API configuration from database,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   *
   * @param wiremockUrl wiremock url with port
   */
  public static void setupDefaultKBConfiguration(String wiremockUrl, Vertx vertx) {
    KbCredentialsTestUtil.saveKbCredentials(wiremockUrl, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
  }

  public static KbCredentials getDefaultKbConfiguration(Vertx vertx) {
    List<KbCredentials> credentials = getKbCredentialsNonSecured(vertx);
    if (credentials.size() != 1) {
      throw new UnsupportedOperationException("There is 0 or more then 1 configuration");
    } else {
      return credentials.get(0);
    }
  }

  public static void clearDataFromTable(Vertx vertx, String tableName) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(SqlQueryHelper.deleteQuery(), getTableName(STUB_TENANT, tableName));
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .execute(query,
        event -> {
          logger().info("Table cleaned up: " + tableName);
          future.complete(null);
        });
    future.join();
  }

  public static Handler<DeliveryContext<LoadHoldingsMessage>> interceptAndContinue(String serviceAddress,
                                                                                   String serviceMethodName,
                                                                                   Consumer<Message<?>> messageConsumer) {
    return messageContext -> {
      Message<?> message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
      }
      messageContext.next();
    };
  }

  public static Handler<DeliveryContext<LoadHoldingsMessage>> interceptAndStop(String serviceAddress,
                                                                               String serviceMethodName,
                                                                               Consumer<Message<?>> messageConsumer) {
    return messageContext -> {
      Message<?> message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
      } else {
        messageContext.next();
      }
    };
  }

  private static boolean messageMatches(String serviceAddress, String serviceMethodName, Message<?> message) {
    return serviceAddress.equals(message.address())
      && serviceMethodName.equals(message.headers().get("action"));
  }

  public static String randomId() {
    return UUID.randomUUID().toString();
  }
}
