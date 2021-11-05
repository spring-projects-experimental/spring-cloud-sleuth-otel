/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.otel.instrument.web.client;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;
import org.springframework.cloud.sleuth.otel.OtelTestSpanHandler;
import org.springframework.cloud.sleuth.otel.bridge.ArrayListSpanProcessor;
import org.springframework.cloud.sleuth.otel.bridge.OtelAccessor;
import org.springframework.cloud.sleuth.test.TestSpanHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = ReactorNettyHttpClientSpringBootTests.Config.class)
@TestPropertySource(properties = "spring.sleuth.otel.propagation.type=custom")
public class ReactorNettyHttpClientSpringBootTests
		extends org.springframework.cloud.sleuth.instrument.web.client.ReactorNettyHttpClientSpringBootTests {

	@Autowired
	HttpClient httpClient;

	@Autowired
	TestSpanHandler handler;

	@Override
	public TraceContext traceContext() {
		return OtelAccessor.traceContext(SpanContext.create(TraceId.fromLongs(1L, 0L), SpanId.fromLong(2L),
				TraceFlags.getSampled(), TraceState.builder().build()));
	}

	@Override
	public void assertSingleB3Header(String b3SingleHeaderReadByServer, FinishedSpan clientSpan, TraceContext parent) {
		Assertions.assertThat(b3SingleHeaderReadByServer)
				.isEqualTo(parent.traceId() + "-" + clientSpan.getSpanId() + "-1");
	}

	@Test
	public void shouldRecordHttpClientAttributes() {
		DisposableServer disposableServer = HttpServer.create().port(0)
				.handle((in, out) -> out.sendString(Flux.just("foo"))).bindNow();

		HttpClientResponse response = httpClient.port(disposableServer.port()).get().uri("/").response().block();

		Assertions.assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);

		FinishedSpan clientSpan = this.handler.takeRemoteSpan(Span.Kind.CLIENT);

		Assertions.assertThat(clientSpan.getTags().get(SemanticAttributes.HTTP_URL.getKey())).isNotEmpty();
		Assertions.assertThat(clientSpan.getTags().get(SemanticAttributes.HTTP_METHOD.getKey())).isNotEmpty();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		TextMapPropagator otelTextMapPropagator() {
			return B3Propagator.injectingSingleHeader();
		}

		@Bean
		OtelTestSpanHandler testSpanHandlerSupplier() {
			return new OtelTestSpanHandler(new ArrayListSpanProcessor());
		}

		@Bean
		Sampler alwaysSampler() {
			return Sampler.alwaysOn();
		}

	}

}
