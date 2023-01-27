package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.NONE;
import static org.junit.Assert.assertEquals;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
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

  private final DefaultLoadServiceFacade defaultLoadServiceFacade =
    new DefaultLoadServiceFacade(1L, 1, 1,
      1, 1, 1,
      vertx);

  @Test
  @SneakyThrows
  public void shouldNotFailOnEmptyStatusFromHoldingsIq() {
    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    assertEquals(NONE, result.get().getStatus());
  }
}
