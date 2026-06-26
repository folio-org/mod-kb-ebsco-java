package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class DefaultLoadServiceFacadeTest {

  @Mock
  private LoadServiceImpl loadService;

  private final Vertx vertx = Vertx.vertx();

  private final DefaultLoadServiceFacade defaultLoadServiceFacade =
    new DefaultLoadServiceFacade(1L, 1, 1, 1, 1, 1, 1, vertx);

  @Test
  @SneakyThrows
  void shouldNotFailOnEmptyStatusFromHoldingsIq() {
    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    assertEquals(NONE, result.get().getStatus());
  }
}
