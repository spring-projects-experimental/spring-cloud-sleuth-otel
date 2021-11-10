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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.sdk.trace.data.SpanData;

import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.SamplerFunction;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.SleuthBaggageProperties;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;
import org.springframework.cloud.sleuth.http.HttpClientHandler;
import org.springframework.cloud.sleuth.http.HttpClientRequest;
import org.springframework.cloud.sleuth.http.HttpClientResponse;
import org.springframework.cloud.sleuth.http.HttpRequest;
import org.springframework.cloud.sleuth.http.HttpRequestParser;
import org.springframework.cloud.sleuth.http.HttpResponseParser;
import org.springframework.cloud.sleuth.http.HttpServerHandler;
import org.springframework.cloud.sleuth.http.HttpServerRequest;
import org.springframework.cloud.sleuth.http.HttpServerResponse;
import org.springframework.cloud.sleuth.instrument.web.SkipPatternProvider;
import org.springframework.cloud.sleuth.propagation.Propagator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Wraps OTel classes in Sleuth representations.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class OtelAccessor {

	private OtelAccessor() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	/**
	 * Creates an OTel Tracer.
	 * @param openTelemetry open telemetry
	 * @param currentTraceContext current trace context
	 * @param sleuthBaggageProperties sleuth baggage properties
	 * @param publisher application publisher
	 * @return tracer
	 */
	public static Tracer tracer(OpenTelemetry openTelemetry, CurrentTraceContext currentTraceContext,
			SleuthBaggageProperties sleuthBaggageProperties, ApplicationEventPublisher publisher) {
		return new OtelTracer(otelTracer(openTelemetry), publisher, new OtelBaggageManager(currentTraceContext,
				sleuthBaggageProperties.getRemoteFields(), sleuthBaggageProperties.getTagFields(), publisher));
	}

	private static io.opentelemetry.api.trace.Tracer otelTracer(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracer("org.springframework.cloud.sleuth");
	}

	/**
	 * Creates an OTel CurrentTraceContext.
	 * @return current trace context
	 */
	public static CurrentTraceContext currentTraceContext() {
		return new OtelCurrentTraceContext();
	}

	/**
	 * Converts OTel SpanContext to TraceContext.
	 * @param spanContext OTel span context
	 * @return converted trace context
	 */
	public static TraceContext traceContext(SpanContext spanContext) {
		return OtelTraceContext.fromOtel(spanContext);
	}

	/**
	 * Converts OTel ContextPropagator to a Propagator.
	 * @param propagators propagators
	 * @param openTelemetry open telemetry
	 * @return OTel propagator
	 */
	public static Propagator propagator(ContextPropagators propagators, OpenTelemetry openTelemetry) {
		return new OtelPropagator(propagators, otelTracer(openTelemetry));
	}

	/**
	 * Creates an HttpClientHandler.
	 * @param openTelemetry open telemetry
	 * @param httpClientRequestParser http client request parser
	 * @param httpClientResponseParser http client response parser
	 * @param samplerFunction sampler function
	 * @return http client handler
	 */
	public static HttpClientHandler httpClientHandler(io.opentelemetry.api.OpenTelemetry openTelemetry,
			@Nullable HttpRequestParser httpClientRequestParser, @Nullable HttpResponseParser httpClientResponseParser,
			SamplerFunction<HttpRequest> samplerFunction,
			HttpClientAttributesExtractor<HttpClientRequest, HttpClientResponse> httpAttributesExtractor) {
		return new OtelHttpClientHandler(openTelemetry, httpClientRequestParser, httpClientResponseParser,
				samplerFunction, httpAttributesExtractor);
	}

	/**
	 * Creates an HttpServerHandler.
	 * @param openTelemetry open telemetry
	 * @param httpServerRequestParser http server request parser
	 * @param httpServerResponseParser http server response parser
	 * @param skipPatternProvider skip pattern provider
	 * @return http server handler
	 */
	public static HttpServerHandler httpServerHandler(io.opentelemetry.api.OpenTelemetry openTelemetry,
			HttpRequestParser httpServerRequestParser, HttpResponseParser httpServerResponseParser,
			SkipPatternProvider skipPatternProvider,
			HttpServerAttributesExtractor<HttpServerRequest, HttpServerResponse> httpAttributesExtractor) {
		return new OtelHttpServerHandler(openTelemetry, httpServerRequestParser, httpServerResponseParser,
				skipPatternProvider, httpAttributesExtractor);
	}

	/**
	 * Converts OTel SpanData to FinishedSpan.
	 * @param spanData OTel span data
	 * @return finished span
	 */
	public static FinishedSpan finishedSpan(SpanData spanData) {
		return new OtelFinishedSpan(spanData);
	}

}
