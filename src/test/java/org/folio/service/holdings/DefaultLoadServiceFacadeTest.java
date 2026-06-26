package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.FAILED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;
import static org.folio.repository.holdings.LoadStatus.NONE;
import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class DefaultLoadServiceFacadeTest {

  private final Vertx vertx = Vertx.vertx();
  private final int pageSize = 2500;
  private final int pageRetryCount = 3;
  @Mock
  private LoadServiceImpl loadService;
  private DefaultLoadServiceFacade defaultLoadServiceFacade;

  @BeforeEach
  void setUp() {
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
  void shouldNotFailOnEmptyStatusFromHoldingsIq() {
    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = result(defaultLoadServiceFacade.getLastLoadingStatus(loadService));

    assertEquals(NONE, result.getStatus());
  }

  @Test
  void shouldMapLoadStatusCompletedCorrectly() {
    var holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .created("2024-01-01 10:00:00")
      .totalCount(5000)
      .build();

    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var holdingsStatus = result(defaultLoadServiceFacade.getLastLoadingStatus(loadService));

    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertEquals("2024-01-01 10:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(5000), holdingsStatus.getTotalCount());
  }

  @Test
  void shouldMapLoadStatusInProgressCorrectly() {
    var holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("In progress")
      .created("2024-01-01 09:00:00")
      .totalCount(3000)
      .build();

    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var holdingsStatus = result(defaultLoadServiceFacade.getLastLoadingStatus(loadService));

    assertEquals(IN_PROGRESS, holdingsStatus.getStatus());
    assertEquals("2024-01-01 09:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(3000), holdingsStatus.getTotalCount());
  }

  @Test
  void shouldMapLoadStatusFailedCorrectly() {
    var holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Failed")
      .created("2024-01-01 08:00:00")
      .totalCount(0)
      .build();

    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var holdingsStatus = result(defaultLoadServiceFacade.getLastLoadingStatus(loadService));

    assertEquals(FAILED, holdingsStatus.getStatus());
    assertEquals("2024-01-01 08:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(0), holdingsStatus.getTotalCount());
  }

  @Test
  void shouldGetLoadingStatusReturnLastLoadingStatusIgnoringTransactionId() {
    var holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .created("2024-01-01 10:00:00")
      .totalCount(5000)
      .build();

    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var holdingsStatus = result(defaultLoadServiceFacade.getLoadingStatus(loadService, "tx-123"));

    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertEquals(Integer.valueOf(5000), holdingsStatus.getTotalCount());
  }

  @Test
  void shouldGetMaxPageSizeReturnConfiguredPageSize() {
    assertEquals(pageSize, defaultLoadServiceFacade.getMaxPageSize());
  }

  @Test
  void shouldCalculateOffsetForPages() {
    verifyOffset(1, 1);
    verifyOffset(2, 2501);
    verifyOffset(3, 5001);
  }

  @Test
  void shouldPopulateHoldingsReturnNullTransactionId() {
    when(loadService.populateHoldingsForce())
      .thenReturn(CompletableFuture.completedFuture(null));

    var actual = result(defaultLoadServiceFacade.populateHoldings(loadService));

    assertNull(actual);
  }

  @Test
  void shouldGetLastLoadingStatusWithDefaultValues() {
    var holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .build();

    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var holdingsStatus = result(defaultLoadServiceFacade.getLastLoadingStatus(loadService));

    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertNull(holdingsStatus.getCreated());
    assertNull(holdingsStatus.getTotalCount());
  }

  @Test
  void shouldGetLastLoadingStatusPreserveCreatedAndCount() {
    var holdingsLoadStatus = HoldingsLoadStatus.builder()
      .status("Completed")
      .created("2024-01-01 10:00:00")
      .totalCount(5000)
      .build();

    when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(holdingsLoadStatus));

    var holdingsStatus = result(defaultLoadServiceFacade.getLastLoadingStatus(loadService));

    assertNotNull(holdingsStatus);
    assertEquals(COMPLETED, holdingsStatus.getStatus());
    assertEquals("2024-01-01 10:00:00", holdingsStatus.getCreated());
    assertEquals(Integer.valueOf(5000), holdingsStatus.getTotalCount());
  }

  private void verifyOffset(int pageNumber, int expectedOffset) {
    var callbackFuture = CompletableFuture.<Void>completedFuture(null);
    IntFunction<CompletableFuture<Void>> offsetLoader = offset -> {
      assertEquals(expectedOffset, offset);
      return callbackFuture;
    };

    defaultLoadServiceFacade.calculateOffset(offsetLoader, pageNumber, pageRetryCount);

    assertTrue(callbackFuture.isDone());
  }
}
