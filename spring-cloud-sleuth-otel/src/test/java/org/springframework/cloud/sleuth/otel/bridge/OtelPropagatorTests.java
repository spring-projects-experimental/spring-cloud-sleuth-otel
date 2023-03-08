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

package org.springframework.cloud.sleuth.otel.bridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.sleuth.BaggageInScope;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

import static org.assertj.core.api.Assertions.assertThat;

class OtelPropagatorTests {

	SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
			.setSampler(io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn()).build();

	OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
			.setPropagators(ContextPropagators.create(B3Propagator.injectingSingleHeader())).build();

	io.opentelemetry.api.trace.Tracer otelTracer = openTelemetrySdk.getTracer("io.micrometer.micrometer-tracing");

	ContextPropagators contextPropagators = ContextPropagators.create(
			TextMapPropagator.composite(W3CBaggagePropagator.getInstance(), W3CTraceContextPropagator.getInstance()));

	OtelPropagator otelPropagator = new OtelPropagator(contextPropagators, otelTracer);

	OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();

	@Test
	void should_propagate_context_with_trace_and_baggage() {
		Map<String, String> carrier = new HashMap<>();
		carrier.put("traceparent", "00-3e425f2373d89640bde06e8285e7bf88-9a5fdefae3abb440-00");
		carrier.put("baggage", "foo=bar");
		Span.Builder extract = otelPropagator.extract(carrier, Map::get);

		Span span = extract.start();

		BaggageInScope baggage = new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(),
				Collections.emptyList(), event -> {
				}).getBaggage(span.context(), "foo").makeCurrent();
		try {
			BDDAssertions.then(baggage.get(span.context())).isEqualTo("bar");
		}
		finally {
			baggage.close();
		}
	}

	@Test
	void should_propagate_context_with_baggage_only() {
		Map<String, String> carrier = new HashMap<>();
		carrier.put("baggage", "foo=bar");
		Span.Builder extract = otelPropagator.extract(carrier, Map::get);

		Span span = extract.start();

		BaggageInScope baggage = new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(),
				Collections.emptyList(), event -> {
				}).getBaggage(span.context(), "foo").makeCurrent();
		try {
			BDDAssertions.then(baggage.get(span.context())).isEqualTo("bar");
		}
		finally {
			baggage.close();
		}
	}

	@Test
	void should_use_created_child_context_in_scope_instead_of_parent() {
		OtelBaggageManager baggageManager = new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList(), Function.identity()::apply);
		OtelTracer tracer = new OtelTracer(otelTracer, Function.identity()::apply, baggageManager);

		Map<String, String> carrier = new HashMap<>();
		carrier.put("traceparent", "00-3e425f2373d89640bde06e8285e7bf88-9a5fdefae3abb440-00");

		Span extracted = otelPropagator.extract(carrier, Map::get).start();
		String expectedSpanId = extracted.context().spanId();

		try (Tracer.SpanInScope ignored = tracer.withSpan(extracted)) {
			assertThat(tracer.currentSpan())
					.extracting(Span::context)
					.returns(expectedSpanId, TraceContext::spanId)
					.returns("3e425f2373d89640bde06e8285e7bf88", TraceContext::traceId)
					.returns("9a5fdefae3abb440", TraceContext::parentId);

			assertThat(tracer.currentTraceContext())
					.isNotNull()
					.extracting(CurrentTraceContext::context)
					.isNotNull()
					.returns(expectedSpanId, TraceContext::spanId)
					.returns("3e425f2373d89640bde06e8285e7bf88", TraceContext::traceId)
					.returns("9a5fdefae3abb440", TraceContext::parentId);
		}
	}
}
