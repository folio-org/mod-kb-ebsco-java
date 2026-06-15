package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.FAILED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;
import static org.folio.repository.holdings.LoadStatus.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import lombok.SneakyThrows;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultLoadServiceFacadeTest {

  @Mock
  private LoadServiceImpl loadService;

  private final Vertx vertx = Vertx.vertx();

  private final int pageSize = 2500;
  private final int pageRetryCount = 3;

  private DefaultLoadServiceFacade defaultLoadServiceFacade;

  @Before
  public void setUp() {
    defaultLoadServiceFacade = new DefaultLoadServiceFacade(
      1L, // statusRetryDelay
      1,  // statusRetryCount
      100, // loadPageRetryDelay
      50, // snapshotRefreshPeriod
      pageRetryCount, // loadPageRetryCount
      pageSize, // loadPageSize
      500, // loadPageSizeMin
      vertx);
  }

  @Test
  @SneakyThrows
  public void shouldNotFailOnEmptyStatusFromHoldingsIq() {
    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    assertEquals(NONE, result.get().getStatus());
  }

  @Test
  @SneakyThrows
  public void shouldMapLoadStatusCompletedCorrectly() {
    HoldingsLoadStatus holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .created("2024-01-01 10:00:00")
      .totalCount(5000)
      .build();

    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    HoldingsStatus holdingsStatus = result.get();

    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertEquals("2024-01-01 10:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(5000), holdingsStatus.getTotalCount());
  }

  @Test
  @SneakyThrows
  public void shouldMapLoadStatusInProgressCorrectly() {
    HoldingsLoadStatus holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("In progress")
      .created("2024-01-01 09:00:00")
      .totalCount(3000)
      .build();

    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    HoldingsStatus holdingsStatus = result.get();

    assertEquals(IN_PROGRESS, holdingsStatus.getStatus());
    assertEquals("2024-01-01 09:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(3000), holdingsStatus.getTotalCount());
  }

  @Test
  @SneakyThrows
  public void shouldMapLoadStatusFailedCorrectly() {
    HoldingsLoadStatus holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Failed")
      .created("2024-01-01 08:00:00")
      .totalCount(0)
      .build();

    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    HoldingsStatus holdingsStatus = result.get();

    assertEquals(FAILED, holdingsStatus.getStatus());
    assertEquals("2024-01-01 08:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(0), holdingsStatus.getTotalCount());
  }

  @Test
  @SneakyThrows
  public void shouldGetLoadingStatusReturnLastLoadingStatusIgnoringTransactionId() {
    HoldingsLoadStatus holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .created("2024-01-01 10:00:00")
      .totalCount(5000)
      .build();

    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var result = defaultLoadServiceFacade.getLoadingStatus(loadService, "tx-123");
    HoldingsStatus holdingsStatus = result.get();

    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertEquals(Integer.valueOf(5000), holdingsStatus.getTotalCount());
  }

  @Test
  public void shouldGetMaxPageSizeReturnConfiguredPageSize() {
    assertEquals(pageSize, defaultLoadServiceFacade.getMaxPageSize());
  }

  @Test
  @SneakyThrows
  public void shouldCalculateOffsetForFirstPage() {
    CompletableFuture<Void> callbackFuture = CompletableFuture.completedFuture(null);

    IntFunction<CompletableFuture<Void>> offsetLoader = offset -> {
      assertEquals(1, offset);
      return callbackFuture;
    };

    defaultLoadServiceFacade.calculateOffset(offsetLoader, 1, pageRetryCount);

    assertTrue(callbackFuture.isDone());
  }

  @Test
  @SneakyThrows
  public void shouldCalculateOffsetForSecondPage() {
    CompletableFuture<Void> callbackFuture = CompletableFuture.completedFuture(null);

    IntFunction<CompletableFuture<Void>> offsetLoader = offset -> {
      assertEquals(2501, offset);
      return callbackFuture;
    };

    defaultLoadServiceFacade.calculateOffset(offsetLoader, 2, pageRetryCount);

    assertTrue(callbackFuture.isDone());
  }

  @Test
  @SneakyThrows
  public void shouldCalculateOffsetForThirdPage() {
    CompletableFuture<Void> callbackFuture = CompletableFuture.completedFuture(null);

    IntFunction<CompletableFuture<Void>> offsetLoader = offset -> {
      assertEquals(5001, offset);
      return callbackFuture;
    };

    defaultLoadServiceFacade.calculateOffset(offsetLoader, 3, pageRetryCount);

    assertTrue(callbackFuture.isDone());
  }

  @Test
  @SneakyThrows
  public void shouldPopulateHoldingsReturnNullTransactionId() {
    Mockito.when(loadService.populateHoldingsForce())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = defaultLoadServiceFacade.populateHoldings(loadService);
    assertNull(result.get());
  }

  @Test
  @SneakyThrows
  public void shouldPopulateHoldingsHandleNullResult() {
    Mockito.when(loadService.populateHoldingsForce())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = defaultLoadServiceFacade.populateHoldings(loadService);
    assertNull(result.get());
  }

  @Test
  @SneakyThrows
  public void shouldGetLastLoadingStatusWithDefaultValues() {
    HoldingsLoadStatus holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .build();

    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    HoldingsStatus holdingsStatus = result.get();

    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertNull(holdingsStatus.getCreated());
    assertNull(holdingsStatus.getTotalCount());
  }

  @Test
  @SneakyThrows
  public void shouldGetLastLoadingStatusPreserveCreatedAndCount() {
    HoldingsLoadStatus holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .created("2024-01-01 10:00:00")
      .totalCount(5000)
      .build();

    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    HoldingsStatus holdingsStatus = result.get();

    assertNotNull(holdingsStatus);
    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertEquals("2024-01-01 10:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(5000), holdingsStatus.getTotalCount());
  }
}
