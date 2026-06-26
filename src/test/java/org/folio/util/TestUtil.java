package org.folio.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.folio.repository.DbUtil.getTableName;
import static org.folio.repository.DbUtil.prepareQuery;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.Message;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.repository.SqlQueryHelper;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@UtilityClass
public class TestUtil {

  public static final String STUB_TENANT = "fs";
  public static final String STUB_TOKEN = "token";

  private static final long TIMEOUT = 30;
  private static final Logger LOG = LogManager.getLogger("KbTestUtil");

  public static <T> T result(CompletableFuture<T> future) {
    try {
      return future.get(TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new TestFutureFailedException(e.getCause() != null ? e.getCause() : e);
    }
  }

  /**
   * Reads file from classpath as String.
   */
  @SneakyThrows
  public static String readFile(String filename) {
    return FileUtils.readFileToString(getFile(filename), UTF_8);
  }

  /**
   * Reads json file from classpath and parses it into object of specified class.
   */
  @SneakyThrows
  public static <T> T readJsonFile(String filename, Class<T> valueType) {
    return new ObjectMapper().readValue(FileUtils.readFileToString(getFile(filename), UTF_8), valueType);
  }

  /**
   * Returns File object corresponding to the file on classpath with specified filename.
   */
  @SneakyThrows
  public static File getFile(String filename) {
    return new File(TestUtil.class.getClassLoader().getResource(filename).toURI());
  }

  public static void clearDataFromTable(Vertx vertx, String tableName) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(SqlQueryHelper.deleteQuery(), getTableName(STUB_TENANT, tableName));
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .execute(query,
        event -> {
          LOG.info("Table cleaned up: {}", tableName);
          future.complete(null);
        });
    future.join();
  }

  public static Handler<DeliveryContext<LoadHoldingsMessage>> interceptAndContinue(
    String serviceAddress, String serviceMethodName, Consumer<Message<?>> messageConsumer) {
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

  public static String randomId() {
    return UUID.randomUUID().toString();
  }

  private static boolean messageMatches(String serviceAddress, String serviceMethodName, Message<?> message) {
    return serviceAddress.equals(message.address())
           && serviceMethodName.equals(message.headers().get("action"));
  }
}
