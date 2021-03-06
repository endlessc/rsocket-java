/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.transport.netty.client;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.rsocket.transport.netty.server.WebsocketServerTransport;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
final class WebsocketClientTransportTest {

  @Test
  @Disabled
  public void testThatSetupWithUnSpecifiedFrameSizeShouldSetMaxFrameSize() {
    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    HttpClient httpClient = Mockito.spy(HttpClient.create());
    Mockito.doAnswer(a -> httpClient).when(httpClient).headers(Mockito.any());
    Mockito.doCallRealMethod().when(httpClient).websocket(captor.capture());

    WebsocketClientTransport clientTransport = WebsocketClientTransport.create(httpClient, "");

    clientTransport.connect(0).subscribe();

    Assertions.assertThat(captor.getValue()).isEqualTo(FRAME_LENGTH_MASK);
  }

  @Test
  @Disabled
  public void testThatSetupWithSpecifiedFrameSizeButLowerThanWsDefaultShouldSetToWsDefault() {
    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    HttpClient httpClient = Mockito.spy(HttpClient.create());
    Mockito.doAnswer(a -> httpClient).when(httpClient).headers(Mockito.any());
    Mockito.doCallRealMethod().when(httpClient).websocket(captor.capture());

    WebsocketClientTransport clientTransport = WebsocketClientTransport.create(httpClient, "");

    clientTransport.connect(65536 - 10000).subscribe();

    Assertions.assertThat(captor.getValue()).isEqualTo(FRAME_LENGTH_MASK);
  }

  @Test
  @Disabled
  public void
      testThatSetupWithSpecifiedFrameSizeButHigherThanWsDefaultShouldSetToSpecifiedFrameSize() {
    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    HttpClient httpClient = Mockito.spy(HttpClient.create());
    Mockito.doAnswer(a -> httpClient).when(httpClient).headers(Mockito.any());
    Mockito.doCallRealMethod().when(httpClient).websocket(captor.capture());

    WebsocketClientTransport clientTransport = WebsocketClientTransport.create(httpClient, "");

    clientTransport.connect(65536 + 10000).subscribe();

    Assertions.assertThat(captor.getValue()).isEqualTo(FRAME_LENGTH_MASK);
  }

  @DisplayName("connects to server")
  @Test
  void connect() {
    InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 0);

    WebsocketServerTransport serverTransport = WebsocketServerTransport.create(address);

    serverTransport
        .start(duplexConnection -> Mono.empty(), 0)
        .flatMap(context -> WebsocketClientTransport.create(context.address()).connect(0))
        .as(StepVerifier::create)
        .expectNextCount(1)
        .verifyComplete();
  }

  @DisplayName("create generates error if server not started")
  @Test
  void connectNoServer() {
    WebsocketClientTransport.create(8000).connect(0).as(StepVerifier::create).verifyError();
  }

  @DisplayName("creates client with BindAddress")
  @Test
  void createBindAddress() {
    assertThat(WebsocketClientTransport.create("test-bind-address", 8000))
        .isNotNull()
        .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("creates client with HttpClient")
  @Test
  void createHttpClient() {
    assertThat(WebsocketClientTransport.create(HttpClient.create(), "/"))
        .isNotNull()
        .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("creates client with HttpClient and path without root")
  @Test
  void createHttpClientWithPathWithoutRoot() {
    assertThat(WebsocketClientTransport.create(HttpClient.create(), "test"))
        .isNotNull()
        .hasFieldOrPropertyWithValue("path", "/test");
  }

  @DisplayName("creates client with InetSocketAddress")
  @Test
  void createInetSocketAddress() {
    assertThat(
            WebsocketClientTransport.create(
                InetSocketAddress.createUnresolved("test-bind-address", 8000)))
        .isNotNull()
        .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("create throws NullPointerException with null bindAddress")
  @Test
  void createNullBindAddress() {
    assertThatNullPointerException()
        .isThrownBy(() -> WebsocketClientTransport.create(null, 8000))
        .withMessage("bindAddress must not be null");
  }

  @DisplayName("create throws NullPointerException with null client")
  @Test
  void createNullHttpClient() {
    assertThatNullPointerException()
        .isThrownBy(() -> WebsocketClientTransport.create(null, "/test-path"))
        .withMessage("client must not be null");
  }

  @DisplayName("create throws NullPointerException with null address")
  @Test
  void createNullInetSocketAddress() {
    assertThatNullPointerException()
        .isThrownBy(() -> WebsocketClientTransport.create((InetSocketAddress) null))
        .withMessage("address must not be null");
  }

  @DisplayName("create throws NullPointerException with null path")
  @Test
  void createNullPath() {
    assertThatNullPointerException()
        .isThrownBy(() -> WebsocketClientTransport.create(HttpClient.create(), null))
        .withMessage("path must not be null");
  }

  @DisplayName("create throws NullPointerException with null URI")
  @Test
  void createNullUri() {
    assertThatNullPointerException()
        .isThrownBy(() -> WebsocketClientTransport.create((URI) null))
        .withMessage("uri must not be null");
  }

  @DisplayName("creates client with port")
  @Test
  void createPort() {
    assertThat(WebsocketClientTransport.create(8000)).isNotNull();
  }

  @DisplayName("creates client with URI")
  @Test
  void createUri() {
    assertThat(WebsocketClientTransport.create(URI.create("ws://test-host")))
        .isNotNull()
        .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("creates client with URI path")
  @Test
  void createUriPath() {
    assertThat(WebsocketClientTransport.create(URI.create("ws://test-host/test")))
        .isNotNull()
        .hasFieldOrPropertyWithValue("path", "/test");
  }

  @DisplayName("sets transport headers")
  @Test
  void setTransportHeader() {
    WebsocketClientTransport.create(8000).setTransportHeaders(Collections::emptyMap);
  }

  @DisplayName("setTransportHeaders throws NullPointerException with null headers")
  @Test
  void setTransportHeadersNullHeaders() {
    assertThatNullPointerException()
        .isThrownBy(() -> WebsocketClientTransport.create(8000).setTransportHeaders(null))
        .withMessage("transportHeaders must not be null");
  }
}
