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

package org.springframework.cloud.sleuth.otel;

import java.io.Closeable;
import java.util.List;
import java.util.regex.Pattern;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.SamplerFunction;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.SleuthBaggageProperties;
import org.springframework.cloud.sleuth.http.HttpClientHandler;
import org.springframework.cloud.sleuth.http.HttpRequestParser;
import org.springframework.cloud.sleuth.http.HttpServerHandler;
import org.springframework.cloud.sleuth.otel.bridge.ArrayListSpanProcessor;
import org.springframework.cloud.sleuth.otel.bridge.OtelAccessor;
import org.springframework.cloud.sleuth.otel.bridge.SpringHttpClientAttributesExtractor;
import org.springframework.cloud.sleuth.otel.bridge.SpringHttpServerAttributesExtractor;
import org.springframework.cloud.sleuth.propagation.Propagator;
import org.springframework.cloud.sleuth.test.TestSpanHandler;
import org.springframework.cloud.sleuth.test.TestTracingAssertions;
import org.springframework.cloud.sleuth.test.TestTracingAware;
import org.springframework.cloud.sleuth.test.TestTracingAwareSupplier;
import org.springframework.cloud.sleuth.test.TracerAware;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Class containing access to all tracing related components.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class OtelTestTracing implements TracerAware, TestTracingAware, TestTracingAwareSupplier, Closeable {

	private final DynamicSampler sampler = new DynamicSampler();

	final ArrayListSpanProcessor spanProcessor = new ArrayListSpanProcessor();

	final ContextPropagators contextPropagators = contextPropagators();

	final io.opentelemetry.api.OpenTelemetry openTelemetry = initializeOtel();

	HttpRequestParser clientRequestParser;

	CurrentTraceContext currentTraceContext = OtelAccessor.currentTraceContext();

	protected ContextPropagators contextPropagators() {
		return ContextPropagators.create(B3Propagator.injectingMultiHeaders());
	}

	OpenTelemetry initializeOtel() {
		return OpenTelemetrySdk.builder().setTracerProvider(otelTracerProvider())
				.setPropagators(this.contextPropagators).build();
	}

	SdkTracerProvider otelTracerProvider() {
		return SdkTracerProvider.builder().addSpanProcessor(spanProcessor).setSampler(sampler).build();
	}

	@Override
	public TracerAware sampler(TraceSampler sampler) {
		this.sampler.setSamplerChoice(sampler);
		return this;
	}

	@Override
	public TracerAware tracing() {
		return this;
	}

	@Override
	public TestSpanHandler handler() {
		return new OtelTestSpanHandler(this.spanProcessor);
	}

	@Override
	public TestTracingAssertions assertions() {
		return new OtelTestTracingAssertions();
	}

	@Override
	public void close() {
		this.spanProcessor.clear();
		this.sampler.reset();
	}

	@Override
	public TestTracingAware tracerTest() {
		return this;
	}

	@Override
	public Tracer tracer() {
		return OtelAccessor.tracer(openTelemetry, this.currentTraceContext, new SleuthBaggageProperties(), publisher());
	}

	@Override
	public CurrentTraceContext currentTraceContext() {
		return OtelAccessor.currentTraceContext();
	}

	@Override
	public Propagator propagator() {
		return OtelAccessor.propagator(this.contextPropagators, openTelemetry);
	}

	@Override
	public HttpServerHandler httpServerHandler() {
		return OtelAccessor.httpServerHandler(openTelemetry, null, null, () -> Pattern.compile(""),
				new SpringHttpServerAttributesExtractor());
	}

	@Override
	public TracerAware clientRequestParser(HttpRequestParser httpRequestParser) {
		this.clientRequestParser = httpRequestParser;
		return this;
	}

	@Override
	public HttpClientHandler httpClientHandler() {
		return OtelAccessor.httpClientHandler(openTelemetry, this.clientRequestParser, null,
				SamplerFunction.alwaysSample(), new SpringHttpClientAttributesExtractor());
	}

	ApplicationEventPublisher publisher() {
		return event -> {
		};
	}

	private static class DynamicSampler implements Sampler {

		private TraceSampler choice = TraceSampler.ON;

		@Override
		public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
				Attributes attributes, List<LinkData> parentLinks) {
			Sampler sampler = Sampler.alwaysOff();
			if (choice == TraceSampler.ON) {
				sampler = Sampler.alwaysOn();
			}
			return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
		}

		@Override
		public String getDescription() {
			return "dynamic test Sampler";
		}

		public void setSamplerChoice(TraceSampler sampler) {
			this.choice = sampler;
		}

		public void reset() {
			this.choice = TraceSampler.ON;
		}

	}

}
