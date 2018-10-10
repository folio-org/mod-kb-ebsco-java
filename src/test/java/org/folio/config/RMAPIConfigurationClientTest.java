package org.folio.config;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.impl.HttpClientResponseImpl;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.client.ConfigurationsClient;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RMAPIConfigurationClientTest {
  private ConfigurationClientProvider configurationClientProvider = mock(ConfigurationClientProvider.class);
  private ConfigurationsClient mockHttpClient = mock(ConfigurationsClient.class);
  private RMAPIConfigurationClient configurationClient = new RMAPIConfigurationClient(configurationClientProvider);

  @Test
  public void shouldCompleteExceptionallyWhenRequestFails() throws Exception {
    HttpClientResponse response = mock(HttpClientResponseImpl.class);
    when(response.statusCode()).thenReturn(400);
    when(configurationClientProvider.createClient(anyString(), anyInt(), anyString(), anyString())).thenReturn(mockHttpClient);
    doAnswer(invocation -> {
      ((Handler<HttpClientResponse>) invocation.getArgument(4)).handle(response);
      return null;
    }).when(mockHttpClient).getEntries(anyString(), anyInt(), anyInt(), any(), any(), any());
    CompletableFuture<RMAPIConfiguration> future = configurationClient.retrieveConfiguration("token", "tenant", "url");
    assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void shouldCompleteExceptionallyWhenHttpClientThrowsException() throws Exception {
    when(configurationClientProvider.createClient(anyString(), anyInt(), anyString(), anyString())).thenReturn(mockHttpClient);
    doThrow(new UnsupportedEncodingException()).when(mockHttpClient).getEntries(anyString(), anyInt(), anyInt(), any(), any(), any());
    CompletableFuture<RMAPIConfiguration> future = configurationClient.retrieveConfiguration("token", "tenant", "url");
    assertTrue(future.isCompletedExceptionally());
  }
}
