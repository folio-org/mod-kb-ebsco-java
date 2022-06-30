package org.folio.service.holdings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import org.folio.holdingsiq.service.LoadService;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
import org.folio.repository.holdings.LoadStatus;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@RunWith(MockitoJUnitRunner.class)
public class AbstractLoadServiceFacadeTest {

  private static final String TEST = "test";

  @Mock
  private LoadServiceImpl loadService;

  private final Vertx vertx = Vertx.vertx();

  private final AbstractLoadServiceFacade loadServiceFacadeSpy =
    Mockito.spy(new AbstractLoadServiceFacade(1L, 3, 1,
      1, 1,vertx) {
      @Override
      protected CompletableFuture<String> populateHoldings(LoadService loadingService) {
        return CompletableFuture.completedFuture(TEST);
      }

      @Override
      protected CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService) {
        return null;
      }

      @Override
      protected CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService, String transactionId) {
        return null;
      }

      @Override
      protected CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService) {
        return null;
      }

      @Override
      protected int getMaxPageSize() {
        return 0;
      }
    });

  @Test
  @SneakyThrows
  public void shouldRetryOnEmptyStatusFromHoldingsIQ() {
    doReturn(getHoldingsStatusFuture(null), getHoldingsStatusFuture(null), getHoldingsStatusFuture(getHoldingsStatus(IN_PROGRESS)))
      .when(loadServiceFacadeSpy)
      .getLastLoadingStatus(any());
    when(loadServiceFacadeSpy.getLoadingStatus(any(), any()))
      .thenReturn(getHoldingsStatusFuture(getHoldingsStatus(COMPLETED)));

    var holdingsStatusFuture = ReflectionTestUtils.<CompletableFuture<HoldingsStatus>>invokeMethod(
      loadServiceFacadeSpy, "populateHoldingsIfNecessary", loadService);

    assertNotNull(holdingsStatusFuture);
    assertEquals(COMPLETED, holdingsStatusFuture.get().getStatus());
    verify(loadServiceFacadeSpy, times(3))
      .getLastLoadingStatus(any());
    verify(loadServiceFacadeSpy, times(1))
      .getLoadingStatus(any(), any());
  }

  @Test
  @SneakyThrows
  public void shouldFailOnRetriesForLastStatusExceeded() {
    doReturn(getHoldingsStatusFuture(null), getHoldingsStatusFuture(null), getHoldingsStatusFuture(null))
      .when(loadServiceFacadeSpy)
      .getLastLoadingStatus(any());

    var holdingsStatusFuture = ReflectionTestUtils.<CompletableFuture<HoldingsStatus>>invokeMethod(
      loadServiceFacadeSpy, "populateHoldingsIfNecessary", loadService);

    assertNotNull(holdingsStatusFuture);
    holdingsStatusFuture.handle((holdingsStatus, throwable) -> {
      assertEquals(IllegalStateException.class, throwable.getCause().getClass());

      return null;
    }).get();

    verify(loadServiceFacadeSpy, times(3))
      .getLastLoadingStatus(any());
  }

  private HoldingsStatus getHoldingsStatus(LoadStatus loadStatus) {
    return HoldingsStatus.builder()
      .status(loadStatus)
      .build();
  }

  private CompletableFuture<HoldingsStatus> getHoldingsStatusFuture(HoldingsStatus holdingsStatus) {
    return CompletableFuture.completedFuture(holdingsStatus);
  }

}
