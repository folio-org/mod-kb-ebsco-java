package org.folio.service.holdings;

import static org.junit.Assert.assertNull;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.folio.holdingsiq.service.impl.LoadServiceImpl;

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
  public void shouldNotFailOnEmptyStatusFromHoldingsIQ() {
    Mockito.when(loadService.getLoadingStatus())
      .thenReturn(CompletableFuture.completedFuture(null));

    var result = defaultLoadServiceFacade.getLastLoadingStatus(loadService);
    assertNull(result.get());
  }

}
